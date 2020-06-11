package online.beapp.afpurchase;

import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.MutableLiveData;
import com.android.billingclient.api.BillingClient;
import com.android.billingclient.api.Purchase;
import com.android.billingclient.api.SkuDetails;
import com.google.android.gms.ads.AdListener;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdSize;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.MobileAds;
import com.google.android.gms.ads.RequestConfiguration;
import com.google.gson.Gson;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import online.beapp.adjustevents.DefaultAdjust;
import online.beapp.adjustevents.User;
import timber.log.Timber;

@SuppressLint("Registered")
public abstract class IAPActivity extends AppCompatActivity {

    private final String TAG = IAPActivity.class.getSimpleName();

    public MutableLiveData<Boolean> loadingListener = new MutableLiveData<>();
    private PurchaseHelper purchaseHelper;
    public String productID = "";
    List<Purchase> purchaseHistory;
    List<SkuDetails> skuDetails = new LinkedList<>();
    boolean isPurchaseQueryPending;
    public boolean isPurchaseActive = false;
    private SkuDetails skuDetailsProduct;
    public boolean isUsingPro = false;
    private DefaultAdjust defaultAdjust;
    private boolean restoreMode = false;
    private boolean oldValuePurchase = false;
    public User user;
    public MutableLiveData<Boolean> purchaseStatus = new MutableLiveData<>();
    public Class<?> defaultIAPActivity;

    public abstract Class<?> setDefaultIAPActivity();

    public IAPActivity() {
        this.defaultIAPActivity = setDefaultIAPActivity();
    }

    private BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Intent intent1 = new Intent(context, defaultIAPActivity);
            intent1.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
            try {
                Uri data = intent.getData();
                if (data != null) {
                    user.setDeeplinkURL(data.toString());
                    Timber.tag(TAG).e("onCreate deep_link: %s", data.toString());
                    catchParams(data);
                    saveUser();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            if (user.isFromDeeplink() && !user.isPurchased())
                context.startActivity(intent1);
            finish();
        }
    };

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (defaultIAPActivity == null) {
            throw new RuntimeException("Please initialize DefaultUAPActivity with your current Billing Screen");
        }
        productID = getResources().getString(R.string.productID);
        String successToken = getResources().getString(R.string.adjust_success_billing);
        String errorToken   = getResources().getString(R.string.adjust_error_event);
        String restoreToken = getResources().getString(R.string.adjust_restore_billing_success);
        defaultAdjust = new DefaultAdjust(this, successToken, errorToken, restoreToken);
        Timber.tag(TAG).e("OnCreate -> readPrefs + initialize PurchaseHelper + loadPackageHelper");
        AdjustKeys adjustKeys = new AdjustKeys(successToken, errorToken, restoreToken);
        purchaseHelper = new PurchaseHelper(this,getPurchaseHelperListener(), adjustKeys);
        readPrefs();
        oldValuePurchase = isPurchaseActive;
        purchaseHelper.serviceConnectionListener.observe(this, aBoolean -> {
            Timber.tag(TAG).e("service status: %s", aBoolean);
            if (aBoolean) {
                loadPackageData();
            } else {
                loadingListener.setValue(false);
            }
        });
        purchaseStatus.observe(this, aBoolean -> {
            if (aBoolean && isPurchaseActive) {
                saveAfterPurchase();
            }
        });
    }

    private void readPrefs() {
        this.isPurchaseActive = SharedPrefs.getIfPurchased(this);
        Timber.tag(TAG).e("read Prefs: isPurchaseActive %s", isPurchaseActive);
    }

    @Override
    protected void onResume() {
        super.onResume();
        registerReceiver();
    }

    @Override
    protected void onRestart() {
        super.onRestart();
    }

    @Override
    protected void onStop() {
        super.onStop();
    }

    @Override
    protected void onDestroy() {
        Timber.tag(TAG).e("onDestroy");
        super.onDestroy();
        if(purchaseHelper != null)
            purchaseHelper.endConnection();
    }

    /**
     * Your listener to handle the various responses of the Purchase helper util
     */
    private PurchaseHelper.PurchaseHelperListener getPurchaseHelperListener() {
        return new PurchaseHelper.PurchaseHelperListener() {
            @Override
            public void onServiceConnected(int resultCode) {
                Timber.tag(TAG).e("OnServiceConnected: %s", resultCode);

                if (isPurchaseQueryPending) {
                    purchaseHelper.getPurchasedItems(BillingClient.SkuType.SUBS);
                    isPurchaseQueryPending = false;
                }
            }

            @Override
            public void onSkuQueryResponse(List<SkuDetails> skuDetailsList) {
                Timber.tag(TAG).e("onSkuQueryResponse Response: %d", skuDetailsList.size());
                skuDetails.clear();
                skuDetails = skuDetailsList;
                for (SkuDetails skudetail :  skuDetailsList) {
                    if (skudetail.getSku().equals(productID)) {
                        skuDetailsProduct = skudetail;
                    }
                    Timber.tag(TAG).e("SKUDetails: sku %s price: %s, original json: %s",skudetail.getSku(), skudetail.getPrice(), skudetail.getOriginalJson());
                }
                Timber.tag(TAG).e("onSkuQueryResponse: purchase status %s", isPurchaseActive);
            }

            @Override
            public void onPrehistoryResponse(List<Purchase> purchasedItems) {
                Timber.tag(TAG).e("onPrehistory Response");
                updatePurchasesStatus(purchasedItems);
            }

            @Override
            public void onPurchasesUpdated(int responseCode, @Nullable List<Purchase> purchases) {
                Timber.tag(TAG).e("onPurchase Updated response code: %d", responseCode);
                switch (responseCode) {
                    case BillingClient.BillingResponseCode.OK:
                        if (purchases != null) {
                            for (Purchase purchase : purchases) {
                                isPurchaseActive = purchase.getSku().equals(IAPActivity.this.productID);
                                SharedPrefs.setPurchased(IAPActivity.this, isPurchaseActive);
                                Timber.tag(TAG).e("Purchased Items: %s", purchase.getSku());
                                defaultAdjust.sendSuccessEvent(purchase,responseCode);
                            }
                        }
                        break;

                    case BillingClient.BillingResponseCode.ITEM_ALREADY_OWNED:
                        if (purchases != null) {
                            for (Purchase purchase : purchases) {
                                isPurchaseActive = purchase.getSku().equals(IAPActivity.this.productID);
                                defaultAdjust.sendRestoreEvent(purchase, BillingClient.BillingResponseCode.ITEM_ALREADY_OWNED);
                            }
                        }
                        break;

                    case BillingClient.BillingResponseCode.ITEM_UNAVAILABLE:
                    case BillingClient.BillingResponseCode.USER_CANCELED:
                    case BillingClient.BillingResponseCode.DEVELOPER_ERROR:
                    case BillingClient.BillingResponseCode.FEATURE_NOT_SUPPORTED:
                    case BillingClient.BillingResponseCode.SERVICE_TIMEOUT:
                    case BillingClient.BillingResponseCode.ERROR:
                        sendEventFail(purchases,responseCode);
                }
                SharedPrefs.setPurchased(IAPActivity.this, isPurchaseActive);
                purchaseStatus.setValue(isPurchaseActive);
            }
        };
    }

    private void sendEventFail(List<Purchase> purchases, int responseCode) {
        Timber.tag(TAG).e("BillingClient.BillingResponseCode.ERROR");
        if (purchases != null) {
            for (Purchase purchase : purchases) {
                if (purchase.getSku().equals(IAPActivity.this.productID)) {
                    defaultAdjust.sendFailureEvent(responseCode, getReasonFor(responseCode), purchase);
                }
            }
        }
        Timber.tag(TAG).e("Error occurred while getting purchases list was found null java.util.Iterator java.util.List.iterator()' on a null object reference ");
        defaultAdjust.sendFailureEvent(responseCode, getReasonFor(responseCode), null );
        Timber.tag(TAG).e("ITEM_UNAVAILABLE");
        isPurchaseActive = false;
    }

    private String getReasonFor(int responseCode) {
        switch (responseCode) {
            case BillingClient.BillingResponseCode.ITEM_UNAVAILABLE:
                return "Item Unavailable";
            case BillingClient.BillingResponseCode.USER_CANCELED:
                return "User Canceled";
            case BillingClient.BillingResponseCode.ITEM_ALREADY_OWNED:
                return "Item Already Owned";
            case BillingClient.BillingResponseCode.DEVELOPER_ERROR:
                return "Developer Error";
            case BillingClient.BillingResponseCode.FEATURE_NOT_SUPPORTED:
                return "Feature not supported";
            case BillingClient.BillingResponseCode.SERVICE_TIMEOUT:
                return "Service Time OUT";
            case BillingClient.BillingResponseCode.ERROR:
                return "Error";
            default:
                return "";
        }

    }

    public void restore() {
        restoreMode = true;
        purchaseHelper.getPurchasedItems(BillingClient.SkuType.SUBS);
    }

    /**
     * 1. To load the data from the assets file
     * 2. Get purchases details for all the items bought within your app.
     */
    private void loadPackageData() {
        Timber.tag(TAG).e("Loading Package Data");
        if (purchaseHelper != null && purchaseHelper.isServiceConnected()) {
            Timber.tag(TAG).e("Loading Package Data: Service Connected");
            List<String> skuList = new ArrayList<>();
            skuList.add(productID);
            purchaseHelper.getSkuDetails(skuList, BillingClient.SkuType.SUBS);
            purchaseHelper.getPurchasedItems(BillingClient.SkuType.SUBS);
            isPurchaseQueryPending = false;
            loadingListener.setValue(true);
        }
        else {
            isPurchaseQueryPending = true;
            loadingListener.setValue(false);
        }
    }

    public void updatePurchasesStatus(List<Purchase> alreadyPurchasedItems) {
        Timber.tag(TAG).e("Updating Purchase Status");
        purchaseHistory = alreadyPurchasedItems;
        for (Purchase purchase : alreadyPurchasedItems) {
            Timber.tag(TAG).e("Updating Purchase Status already contain purchased item: %s", purchase.getSku());
            isPurchaseActive = purchase.getSku().equals(productID);
            if (!oldValuePurchase || restoreMode)
                defaultAdjust.sendRestoreEvent(purchase,DefaultAdjust.ResponseCode.OK);
        }
        if (alreadyPurchasedItems.isEmpty() && restoreMode) {
            defaultAdjust.sendFailureEvent(DefaultAdjust.ResponseCode.ITEM_NOT_OWNED,"No Purchase History Founded", null);
        }
        SharedPrefs.setPurchased(this, isPurchaseActive);
    }


    /**
     * Your listener to handle the click actions of the listing cards
     */
    public void OnPurchaseClickAction() {
        Timber.tag(TAG).e("On Purchase Click Action");
        if (skuDetails != null) {
            Timber.tag(TAG).e("On Purchase Click Action: SkuDetails not empty: %s", skuDetails.size());
            for (SkuDetails skuDetail : skuDetails) {
                Timber.tag(TAG).e("On Purchase Click Action check every SkuDetail price: %s, sku: %s", skuDetail.getPrice(), skuDetail.getSku());
                if (skuDetail.getSku().equals(productID))
                    skuDetailsProduct = skuDetail;
                else
                    defaultAdjust.sendFailureEvent(DefaultAdjust.ResponseCode.ITEM_UNAVAILABLE,"Item Not Found", null);
            }
        }
        purchaseHelper.launchBillingFLow(skuDetailsProduct);
    }

    public void saveAfterPurchase() {
        Timber.tag(TAG).e("Save After Purchase");
    }

    private void startInAppBilling() {
        Intent intent1 = new Intent(this, defaultIAPActivity);
        startActivity(intent1);
    }

    @Override
    protected void onPause() {
        super.onPause();
        this.unregisterReceiver(this.broadcastReceiver);
    }

    private void registerReceiver() {
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction("org.smartmobiletech.pixyEditor");
        this.registerReceiver(this.broadcastReceiver, intentFilter);
    }

    /**
     * loading user from shared Preferences as JSON and mapping into a USER class
     * @return User class
     */
    public User loadUser() {
        Gson gson = new Gson();
        String userJSON = SharedPrefs.getUser(this);
        if (userJSON.equals("empty")) {
            return new User(false,false);
        }
        return gson.fromJson(SharedPrefs.getUser(this),User.class);
    }

    /**
     * Saving user info after converting the USER class object to json through GSON and apply it in shared Preferences
     */
    public void saveUser() {
        Gson gson = new Gson();
        String userJSON = gson.toJson(user);
        SharedPrefs.setUser(this, userJSON);
        Timber.tag(TAG).e("User: %s",userJSON);
    }

    /**
     * split the content of {@param url} and separate keys and value using ending .utf8
     * @param url {@link Uri}
     * @return {@link Map} contains keys and values
     * @throws UnsupportedEncodingException throw exception if the uri data encoding does not comfort the current one using
     */
    public static Map<String, String> splitQuery(Uri url) throws UnsupportedEncodingException {
        Map<String, String> query_pairs = new LinkedHashMap<>();
        String query = url.getQuery();
        assert query != null;
        String[] pairs = query.split("&");
        for (String pair : pairs) {
            int idx = pair.indexOf("=");
            query_pairs.put(URLDecoder.decode(pair.substring(0, idx), "UTF-8"), URLDecoder.decode(pair.substring(idx + 1), "UTF-8"));
        }
        return query_pairs;
    }
    /**
     * load the data inside {@param data} and after getting the query from {@link #splitQuery(Uri)} assign every if key if found in the {@link Uri}
     * into the current user defined
     * @param data is {@link Uri} fetched from Intent
     */
    public void catchParams(Uri data) {
        try {
            Map<String, String> query = splitQuery(data);
            for (Map.Entry<String, String> entry : query.entrySet()) {
                System.out.println(entry);
                Timber.tag(TAG).e("Entry: %s", entry);
            }

            String isPurchased = query.get("isPurchased");
            if (isPurchased != null) {
                boolean isPro = isPurchased.equals("true");
                user.setPurchased(isPro);
            }

            user.setFromDeeplink(true);
            String sEnd = query.get("sEnd");
            if (sEnd != null) {
                user.setEndSubscription();
            }
            String sStart = query.get("sStart");
            if(sStart != null) {
                user.setStartSubscription();
            }

        }catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void loadAds(View container) {
        if (!isPurchaseActive) {
            Timber.e("Ads loading…");
            MobileAds.initialize(this);
            AdView mAdView = new AdView(this);
            mAdView.setAdSize(AdSize.BANNER);
            //TODO replace banner id
            mAdView.setAdUnitId(getResources().getString(R.string.banner_id));
            ((LinearLayout) container).addView(mAdView);
            ArrayList<String> testDevice = new ArrayList<>();
            testDevice.add(AdRequest.DEVICE_ID_EMULATOR);
            RequestConfiguration configuration = new RequestConfiguration.Builder().setTestDeviceIds(testDevice).build();
            MobileAds.setRequestConfiguration(configuration);
            AdRequest adRequest = new AdRequest.Builder().build();

            mAdView.loadAd(adRequest);
            mAdView.setAdListener(new AdListener() {
                @Override
                public void onAdClosed() {
                    super.onAdClosed();
                }
                @Override
                public void onAdFailedToLoad(int i) {
                    super.onAdFailedToLoad(i);
                    container.setVisibility(View.GONE);
                }
                @Override
                public void onAdLeftApplication() {
                    super.onAdLeftApplication();
                }

                @Override
                public void onAdOpened() {
                    super.onAdOpened();
                }

                @Override
                public void onAdLoaded() {
                    super.onAdLoaded();
                    container.setVisibility(View.VISIBLE);
                }
            });
        }
    }
}
