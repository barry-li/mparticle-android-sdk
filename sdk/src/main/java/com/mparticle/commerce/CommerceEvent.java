package com.mparticle.commerce;


import com.mparticle.MParticle;
import com.mparticle.internal.ConfigManager;
import com.mparticle.internal.MPUtility;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * The canonical object used to measure a commerce-related datapoint.
 *
 * This class is highly flexible and can be used to encapsulate user-interactions with Products, Promotions, or Impressions of Products.
 *
 * CommerceEvent objects should generally be considered immutable, several Builder classes are provided to help you construct new CommerceEvent objects.
 *
 * The {@link CommerceApi} helper methods, accessed via {@code MParticle.getInstance().Commerce()}, are a wrapper around this class and will generate and log CommerceEvent objects
 * for you.
 *
 * @see com.mparticle.commerce.CommerceEvent.Builder
 * @see MParticle#logEvent(CommerceEvent)
 *
 */
public final class CommerceEvent {
    private String mEventName;
    private List<Impression> mImpressions;
    private String mProductAction;
    private String mPromotionAction;
    private Map<String, String> customAttributes;
    private List<Promotion> promotionList;
    private List<Product> productList;
    private Integer mCheckoutStep;
    private String mCheckoutOptions;
    private String mProductListName;
    private String mProductListSource;
    private String mCurrency;
    private TransactionAttributes mTransactionAttributes;
    private String mScreen;
    private Boolean mNonIteraction;

    private CommerceEvent(Builder builder) {
        super();
        mProductAction = builder.mProductAction;
        mPromotionAction = builder.mPromotionAction;
        customAttributes = builder.customAttributes;
        promotionList = builder.promotionList;
        if (builder.productList != null) {
            productList = new LinkedList<>(builder.productList);
        }
        mCheckoutStep = builder.mCheckoutStep;
        mCheckoutOptions = builder.mCheckoutOptions;
        mProductListName = builder.mProductListName;
        mProductListSource = builder.mProductListSource;
        mCurrency = builder.mCurrency;
        mTransactionAttributes = builder.mTransactionAttributes;
        mScreen = builder.mScreen;
        mImpressions = builder.mImpressions;
        mNonIteraction = builder.mNonIteraction;
        mEventName = builder.mEventName;

        boolean devMode = MParticle.getInstance().getEnvironment().equals(MParticle.Environment.Development);

        if (MPUtility.isEmpty(mProductAction)
                && MPUtility.isEmpty(mPromotionAction)
                && (mImpressions == null || mImpressions.size() == 0)) {
            if (devMode) {
                throw new IllegalStateException("CommerceEvent must be created with either a product action. promotion action, or an impression");
            } else {
                ConfigManager.log(MParticle.LogLevel.ERROR, "CommerceEvent must be created with either a product action, promotion action, or an impression");
            }
        }

        if (mProductAction != null) {
            if (mProductAction.equalsIgnoreCase(Product.PURCHASE) ||
                    mProductAction.equalsIgnoreCase(Product.REFUND)) {
                if (mTransactionAttributes == null || MPUtility.isEmpty(mTransactionAttributes.getId())) {
                    String message = "CommerceEvent with action " + mProductAction + " must include a TransactionAttributes object with a transaction ID.";
                    if (devMode) {
                        throw new IllegalStateException(message);
                    } else {
                        ConfigManager.log(MParticle.LogLevel.ERROR, message);
                    }
                }
            }
            if (promotionList != null && promotionList.size() > 0) {
                if (devMode) {
                    throw new IllegalStateException("Product CommerceEvent should not contain Promotions.");
                } else {
                    ConfigManager.log(MParticle.LogLevel.ERROR, "Product CommerceEvent should not contain Promotions.");
                }
            }
            if (!mProductAction.equals(Product.ADD_TO_CART)
                    && !mProductAction.equals(Product.ADD_TO_WISHLIST)
                    && !mProductAction.equals(Product.CHECKOUT)
                    && !mProductAction.equals(Product.CHECKOUT_OPTION)
                    && !mProductAction.equals(Product.CLICK)
                    && !mProductAction.equals(Product.DETAIL)
                    && !mProductAction.equals(Product.PURCHASE)
                    && !mProductAction.equals(Product.REFUND)
                    && !mProductAction.equals(Product.REMOVE_FROM_CART)
                    && !mProductAction.equals(Product.REMOVE_FROM_WISHLIST)) {
                ConfigManager.log(MParticle.LogLevel.ERROR, "CommerceEvent created with unrecognized Product action: " + mProductAction);
            }
        }else if (mPromotionAction != null ) {
            if (productList != null && productList.size() > 0) {
                if (devMode) {
                    throw new IllegalStateException("Promotion CommerceEvent should not contain Products.");
                }else {
                    ConfigManager.log(MParticle.LogLevel.ERROR, "Promotion CommerceEvent should not contain Products.");
                }
            }
            if (!mPromotionAction.equals(Promotion.VIEW)
                    && !mPromotionAction.equals(Promotion.CLICK)) {
                ConfigManager.log(MParticle.LogLevel.ERROR, "CommerceEvent created with unrecognized Promotion action: " + mProductAction);
            }
        }else {
            if (productList != null && productList.size() > 0) {
                if (devMode) {
                    throw new IllegalStateException("Impression CommerceEvent should not contain Products.");
                }else {
                    ConfigManager.log(MParticle.LogLevel.ERROR, "Impression CommerceEvent should not contain Products.");
                }
            }
            if (promotionList != null && promotionList.size() > 0) {
                if (devMode) {
                    throw new IllegalStateException("Impression CommerceEvent should not contain Promotions.");
                } else {
                    ConfigManager.log(MParticle.LogLevel.ERROR, "Impression CommerceEvent should not contain Promotions.");
                }
            }
        }


        if (mTransactionAttributes == null || mTransactionAttributes.getRevenue() == null) {
            double transactionRevenue = 0;
            if (mTransactionAttributes == null) {
                mTransactionAttributes = new TransactionAttributes();
            } else {
                Double shipping = mTransactionAttributes.getShipping();
                Double tax = mTransactionAttributes.getTax();
                transactionRevenue += (shipping != null ? shipping : 0);
                transactionRevenue += (tax != null ? tax : 0);
            }
            if (productList != null) {
                for (Product product : productList) {
                    if (product != null) {
                        double price = product.getUnitPrice();
                        price *= product.getQuantity();
                        transactionRevenue += price;
                    }
                }
            }
            mTransactionAttributes.setRevenue(transactionRevenue);
        }
    }

    private CommerceEvent() {

    }

    @Override
    public String toString() {
        try {
            JSONObject eventJson = new JSONObject();
            if (mScreen != null) {
                eventJson.put("sn", mScreen);
            }
            if (mNonIteraction != null) {
                eventJson.put("ni", mNonIteraction.booleanValue());
            }
            if (mProductAction != null) {
                JSONObject productAction = new JSONObject();
                eventJson.put("pd", productAction);
                productAction.put("an", mProductAction);
                if (mCheckoutStep != null) {
                    productAction.put("cs", mCheckoutStep);
                }
                if (mCheckoutOptions != null) {
                    productAction.put("co", mCheckoutOptions);
                }
                if (mProductListName != null) {
                    productAction.put("pal", mProductListName);
                }
                if (mProductListSource != null) {
                    productAction.put("pls", mProductListSource);
                }
                if (mTransactionAttributes != null) {
                    if (mTransactionAttributes.getId() != null) {
                        productAction.put("ti", mTransactionAttributes.getId());
                    }
                    if (mTransactionAttributes.getAffiliation() != null) {
                        productAction.put("ta", mTransactionAttributes.getAffiliation());
                    }
                    if (mTransactionAttributes.getRevenue() != null) {
                        productAction.put("tr", mTransactionAttributes.getRevenue());
                    }
                    if (mTransactionAttributes.getTax() != null) {
                        productAction.put("tt", mTransactionAttributes.getTax());
                    }
                    if (mTransactionAttributes.getShipping() != null) {
                        productAction.put("ts", mTransactionAttributes.getShipping());
                    }
                    if (mTransactionAttributes.getCouponCode() != null) {
                        productAction.put("tcc", mTransactionAttributes.getCouponCode());
                    }
                }
                if (productList != null && productList.size() > 0) {
                    JSONArray products = new JSONArray();
                    for (int i = 0; i < productList.size(); i++) {
                        products.put(new JSONObject(productList.get(i).toString()));
                    }
                    productAction.put("pl", products);
                }


            } else {
                JSONObject promotionAction = new JSONObject();
                eventJson.put("pm", promotionAction);
                promotionAction.put("an", mPromotionAction);
                if (promotionList != null && promotionList.size() > 0) {
                    JSONArray promotions = new JSONArray();
                    for (int i = 0; i < promotionList.size(); i++) {
                        promotions.put(new JSONObject(promotionList.get(i).toString()));
                    }
                    promotionAction.put("pl", promotions);
                }
            }
            if (mImpressions != null && mImpressions.size() > 0) {
                JSONArray impressions = new JSONArray();
                for (Impression impression : mImpressions) {
                    JSONObject impressionJson = new JSONObject();
                    if (impression.getListName() != null) {
                        impressionJson.put("pil", impression.getListName());
                    }
                    if (impression.getProducts() != null && impression.getProducts() .size() > 0) {
                        JSONArray productsJson = new JSONArray();
                        impressionJson.put("pl", productsJson);
                        for (Product product : impression.getProducts()) {
                            productsJson.put(new JSONObject(product.toString()));
                        }
                    }
                    if (impressionJson.length() > 0) {
                        impressions.put(impressionJson);
                    }
                }
                if (impressions.length() > 0) {
                    eventJson.put("pi", impressions);
                }
            }
            return eventJson.toString();

        } catch (JSONException jse) {

        }
        return super.toString();
    }

    /**
     *
     * Retrieve the Map of custom attributes of the event.
     *
     * @return returns a Map of custom attributes, or null if no custom attributes are set
     *
     * @see com.mparticle.commerce.CommerceEvent.Builder#customAttributes(Map)
     */
    public Map<String, String> getCustomAttributes() {
        return customAttributes;
    }

    /**
     * Retrieve the Screen where the event occurred
     *
     * @return the String descriptor/name of the Screen where this event occurred, or null if this is not set
     *
     * @see com.mparticle.commerce.CommerceEvent.Builder#screen(String)
     */
    public String getScreen() {
        return mScreen;
    }

    /**
     * Retrieve the boolean indicating if the event was triggered by a user
     *
     * @return a Boolean indicating if this event was triggered by a user, or null if not set
     *
     * @see com.mparticle.commerce.CommerceEvent.Builder#nonInteraction(boolean)
     *
     */
    public Boolean getNonInteraction() {
        return mNonIteraction;
    }

    /**
     * Retrieve the Product action of the event. CommerceEvents are either Product, Promotion, or Impression based. The Product Action
     * will be null in the case of Promotion and Impression CommerceEvents.
     *
     * @return a String indicating the Product action, or null if this is not a Product-based CommerceEvent
     *
     * @see <code><a href="CommerceEvent.Builder.html#CommerceEvent.Builder(java.lang.String,%20com.mparticle.commerce.Product)">Builder(java.lang.String, com.mparticle.commerce.Product)</a></code>
     * @see Product
     */
    public String getProductAction() {
        return mProductAction;
    }

    /**
     * Retrieve the Checkout Step of the CommerceEvent. This should typically only be set in the case of a {@link Product#CHECKOUT} CommerceEvent.
     *
     * @return an Integer representing the step, or null if none is set
     */
    public Integer getCheckoutStep() {
        return mCheckoutStep;
    }

    /**
     * Retrieve the Checkout options of the CommerceEvent. This describes custom options that a user may select at particular steps in the checkout process.
     *
     * @return a String describing any checkout options, or null if none are set
     */
    public String getCheckoutOptions() {
        return mCheckoutOptions;
    }

    /**
     * Retrieve the Product List Name associated with the CommerceEvent. This value should be set for {@link Product#DETAIL} and {@link Product#CLICK} CommerceEvents.
     *
     * @return the product list name, or null if not set.
     *
     * @see com.mparticle.commerce.CommerceEvent.Builder#productListName(String)
     */
    public String getProductListName() {
        return mProductListName;
    }

    /**
     * Retrieve the product list source associated with the CommerceEvent. This value should be set for {@link Product#DETAIL} and {@link Product#CLICK} CommerceEvents.
     *
     * @return the product list source, or null if not set.
     *
     * @see com.mparticle.commerce.CommerceEvent.Builder#productListSource(String)
     */
    public String getProductListSource() {
        return mProductListSource;
    }

    /**
     * Retrieve the {@link TransactionAttributes} object associated with the CommerceEvent
     *
     * @return the TransactionAttributes object, or null if not set.
     *
     * @see com.mparticle.commerce.CommerceEvent.Builder#transactionAttributes(TransactionAttributes)
     */
    public TransactionAttributes getTransactionAttributes() {
        return mTransactionAttributes;
    }

    /**
     * Retrieve the list of Products to which the CommerceEvent applies. This should only be set for Product-type CommerceEvents
     *
     * @return the list of Products, or null if not set.
     *
     * @see <code><a href="CommerceEvent.Builder.html#CommerceEvent.Builder(java.lang.String,%20com.mparticle.commerce.Product)">Builder(java.lang.String, com.mparticle.commerce.Product)</a></code>
     * @see com.mparticle.commerce.CommerceEvent.Builder#addProduct(Product)
     * @see com.mparticle.commerce.CommerceEvent.Builder#products(List)
     */
    public List<Product> getProducts() {
        if (productList == null) {
            return null;
        }
        return Collections.unmodifiableList(productList);
    }

    /**
     * Retrieve the Promotion action of the CommerceEvent. CommerceEvents are either Product, Promotion, or Impression based. The Promotion Action
     * will be null in the case of Product and Impression CommerceEvents.
     *
     * @return a String indicating the Promotion action, or null if this is not a Promotion-based CommerceEvent
     *
     * @see <code><a href="CommerceEvent.Builder.html#CommerceEvent.Builder(java.lang.String,%20com.mparticle.commerce.Promotion)">Builder(java.lang.String, com.mparticle.commerce.Promotion)</a></code>
     * @see Promotion for supported product actions
     */
    public String getPromotionAction() {
        return mPromotionAction;
    }

    /**
     * Retrieve the {@link Promotion} list associated with the CommerceEvent.
     *
     * @return returns an unmodifiable List of Promotions, or null if this is a {@link Product} or {@link Impression} based {@link CommerceEvent}
     *
     * @see <code><a href="CommerceEvent.Builder.html#CommerceEvent.Builder(java.lang.String,%20com.mparticle.commerce.Promotion)">Builder(java.lang.String, com.mparticle.commerce.Promotion)</a></code>
     * @see com.mparticle.commerce.CommerceEvent.Builder#addPromotion(Promotion)
     * @see com.mparticle.commerce.CommerceEvent.Builder#promotions(List)
     */
    public List<Promotion> getPromotions() {
        if (promotionList == null) {
            return null;
        }
        return Collections.unmodifiableList(promotionList);
    }

    @Override
    public boolean equals(Object o) {
        return o != null && o.toString().equals(toString());
    }

    /**
     * Retrieve the {@link Impression} list associated with the CommerceEvent.
     *
     * @return returns an unmodifiable List of Impressions, or null if this is a {@link Product} or {@link Promotion} based {@link CommerceEvent}
     *
     * @see <code><a href="CommerceEvent.Builder.html#CommerceEvent.Builder(java.lang.String,%20com.mparticle.commerce.Impression)">Builder(java.lang.String, com.mparticle.commerce.Impression)</a></code>
     * @see com.mparticle.commerce.CommerceEvent.Builder#addImpression(Impression)
     * @see com.mparticle.commerce.CommerceEvent.Builder#impressions(List)
     */
    public List<Impression> getImpressions() {
        if (mImpressions == null) {
            return null;
        }
        return Collections.unmodifiableList(mImpressions);
    }

    /**
     * Retrieve the currency code associated with the CommerceEvent.
     *
     * @return returns a String representing the currency code, or null if not set.
     */
    public String getCurrency() {
        return mCurrency;
    }

    /**
     * Retrieve the event name associated with the CommerceEvent. Most service providers do not require that this value is set.
     *
     * @return the name associated with the CommerceEvent, or null if not set.
     */
    public String getEventName() {
        return mEventName;
    }

    /**
     * The Builder class for {@link CommerceEvent}. Use this class to create immutable CommerceEvents than can then be logged.
     *
     * There are 3 types of {@link CommerceEvent}:
     * <ul>
     *  <li>{@link Product}</li>
     *  <li>{@link Promotion}</li>
     *  <li>{@link Impression}</li>
     * </ul>
     * <br>
     *  This class provides a constructor for each type and will verify the contents of the event when {@link Builder#build()} is called.
     * <br><br>
     *  <b>Sample Product event</b>
     *  <br>
     * <pre>
     * {@code
     *
     *  Product product = new Product.Builder("Foo name", "Foo sku", 100.00).quantity(4).build();
     *  CommerceEvent event = new CommerceEvent.Builder(Product.PURCHASE, product)
     *          .transactionAttributes(new TransactionAttributes().setId("bar-transaction-id")
     *          .setRevenue(product.getTotalAmount()))
     *          .build();
     *  MParticle.getInstance().logEvent(event);
     * }
     * </pre>
     * <br>
     * <br>
     * <b>Sample Promotion event</b><br>
     * <pre>
     * {@code
     *
     *  Promotion promotion = new Promotion().setCreative("foo-creative").setName("bar campaign");
     *  CommerceEvent event = new CommerceEvent.Builder(Promotion.VIEW, promotion).build();
     *  MParticle.getInstance().logEvent(event);
     * }
     * </pre>
     * <br>
     * <br>
     * <b>Sample Impression event</b><br>
     * <pre>
     * {@code
     *
     *   Product product = new Product.Builder("Foo name", "Foo sku", 100.00).quantity(4).build();
     *   Impression impression = new Impression("foo-list-name", product);
     *   CommerceEvent event = new CommerceEvent.Builder(impression).build();
     *   MParticle.getInstance().logEvent(event);
     * }
     * </pre>
     */
    public static class Builder {

        private final String mProductAction;
        private final String mPromotionAction;
        private Map<String, String> customAttributes = null;
        private List<Promotion> promotionList = null;
        private List<Product> productList = null;
        private Integer mCheckoutStep = null;
        private String mCheckoutOptions = null;
        private String mProductListName = null;
        private String mProductListSource = null;
        private String mCurrency = null;
        private TransactionAttributes mTransactionAttributes = null;
        private String mScreen = null;
        private Boolean mNonIteraction;
        private List<Impression> mImpressions;
        private String mEventName;

        private Builder() {
            mProductAction = mPromotionAction = null;
        }

        /**
         * Constructor for a {@link Product}-based CommerceEvent.
         *
         * @param productAction a String representing the action that the user performed. This must be one of the String constants defined by the {@link Product} class. Must not be null.
         * @param product at {@link Product} object to associate with the given action. Must not be null.
         *
         *
         * @see Product#CLICK
         * @see Product#DETAIL
         * @see Product#CHECKOUT
         * @see Product#ADD_TO_CART
         * @see Product#REMOVE_FROM_CART
         * @see Product#ADD_TO_WISHLIST
         * @see Product#REMOVE_FROM_WISHLIST
         * @see Product#CHECKOUT
         * @see Product#CHECKOUT_OPTION
         * @see Product#PURCHASE
         *
         */
        public Builder(String productAction, Product product) {
            mProductAction = productAction;
            mPromotionAction = null;
            addProduct(product);
        }

        /**
         * Constructor for a {@link Promotion}-based CommerceEvent.
         *
         * @param promotionAction a String representing the action that use user performed. This must be on the String constants defined by the {@link Promotion} class. Must not be null.
         * @param promotion at least 1 {@link Promotion} object to associate with the given action. Must not be null.
         *
         * @see Promotion#CLICK
         * @see Promotion#VIEW
         */
        public Builder(String promotionAction, Promotion promotion) {
            mProductAction = null;
            mPromotionAction = promotionAction;
            addPromotion(promotion);
        }

        /**
         * Constructor for a {@link Impression}-based CommerceEvent
         *
         * @param impression the impression to associate with this event. Must not be null.
         */
        public Builder(Impression impression) {
            addImpression(impression);
            mPromotionAction = null;
            mProductAction = null;
        }

        /**
         * Convenience copy-constructor. Use this to convert or mutate a given CommerceEvent.
         *
         * @param event an existing CommerceEvent. Must not be null.
         */
        public Builder(CommerceEvent event) {
            mProductAction = event.getProductAction();
            mPromotionAction = event.getPromotionAction();
            if (event.getCustomAttributes() != null) {
                Map<String, String> shallowCopy = new HashMap<String, String>();
                shallowCopy.putAll(event.getCustomAttributes());
                customAttributes = shallowCopy;
            }
            if (event.getPromotions() != null) {
                for (Promotion promotion : event.getPromotions()) {
                    addPromotion(new Promotion(promotion));
                }
            }
            if (event.getProducts() != null) {
                for (Product product : event.getProducts()) {
                    addProduct(new Product.Builder(product).build());
                }
            }
            mCheckoutStep = event.getCheckoutStep();
            mCheckoutOptions = event.getCheckoutOptions();
            mProductListName = event.getProductListName();
            mProductListSource = event.getProductListSource();
            mCurrency = event.getCurrency();
            if (event.getTransactionAttributes() != null) {
                mTransactionAttributes = new TransactionAttributes(event.getTransactionAttributes());
            }
            mScreen = event.mScreen;
            mNonIteraction = event.mNonIteraction;
            if (event.getImpressions() != null) {
                for (Impression impression : event.getImpressions()) {
                    addImpression(new Impression(impression));
                }
            }
            mEventName = event.getEventName();

        }

        /**
         * Set the screen to associate with this event
         *
         * @param screenName a String name or description of the screen where this event occurred.
         * @return returns this Builder for easy method chaining.
         */
        public Builder screen(String screenName) {
            mScreen = screenName;
            return this;
        }

        /**
         * Add a {@link Product} to this CommerceEvent.
         *
         * <i>This should only be called for {@link Product}-based CommerceEvents created with <code><a href="CommerceEvent.Builder.html#CommerceEvent.Builder(java.lang.String,%20com.mparticle.commerce.Product)">Builder(java.lang.String, com.mparticle.commerce.Product)</a></code> </i>
         *
         * @param product the {@link Product} to add to this CommerceEvent.
         * @return returns this Builder for easy method chaining.
         */
        public Builder addProduct(Product product) {
            if (productList == null) {
                productList = new LinkedList<Product>();
            }
            productList.add(product);
            return this;
        }

        /**
         * Associate a {@link TransactionAttributes} object with this event. This will typically be used with {@link Product}-based CommerceEvents.
         *
         * <i>For {@link Product#PURCHASE} and {@link Product#REFUND} CommerceEvents, this is required to be set.t</i>
         *
         * @param attributes the {@link TransactionAttributes} object
         * @return returns this Builder for easy method chaining.
         */
        public Builder transactionAttributes(TransactionAttributes attributes) {
            mTransactionAttributes = attributes;
            return this;
        }

        /**
         * Set the ISO 4217 currency code to associate with this event.
         *
         * @param currency an ISO 4217 String
         * @return returns this Builder for easy method chaining.
         */
        public Builder currency(String currency) {
            mCurrency = currency;
            return this;
        }

        /**
         * Set this CommerceEvent to be measured as non-user-triggered.
         *
         * @param userTriggered a Boolean indicating if a user actually performed this event
         * @return returns this Builder for easy method chaining.
         */
        public Builder nonInteraction(boolean userTriggered) {
            mNonIteraction = userTriggered;
            return this;
        }

        /**
         * Associate a Map of custom attributes with this event.
         *
         * @param attributes the Map of attributes
         * @return returns this Builder for easy method chaining.
         */
        public Builder customAttributes(Map<String, String> attributes) {
            customAttributes = attributes;
            return this;
        }

        /**
         * Add a {@link Promotion} to this CommerceEvent.
         *
         * <i>This should only be called for {@link Promotion}-based CommerceEvents created with <code><a href="CommerceEvent.Builder.html#CommerceEvent.Builder(java.lang.String,%20com.mparticle.commerce.Promotion)">Builder(java.lang.String, com.mparticle.commerce.Promotion)</a></code></i>
         *
         * @param promotion the {@link Promotion} to add to this CommerceEvent.
         * @return returns this Builder for easy method chaining.
         */
        public Builder addPromotion(Promotion promotion) {
            if (promotionList == null) {
                promotionList = new LinkedList<Promotion>();
            }
            promotionList.add(promotion);
            return this;
        }

        /**
         * Set the checkout step of this event. Should be used with the {@link Product#CHECKOUT} and {@link Product#CHECKOUT_OPTION} actions.
         *
         * @param step the Integer step
         * @return returns this Builder for easy method chaining.
         */
        public Builder checkoutStep(Integer step) {
            if (step == null || step >= 0) {
                mCheckoutStep = step;
            }
            return this;
        }

        /**
         * Add a {@link Impression} to this CommerceEvent.
         *
         * <i>This should only be called for {@link Impression}-based CommerceEvents created with <code><a href="CommerceEvent.Builder.html#CommerceEvent.Builder(com.mparticle.commerce.Impression)">Builder(com.mparticle.commerce.Impression)</a></code> </i>
         *
         * @param impression the {@link Impression} to add to the CommerceEvent.
         * @return returns this Builder for easy method chaining.
         */
        public Builder addImpression(Impression impression) {
            if (impression != null) {
                if (mImpressions == null) {
                    mImpressions = new LinkedList<Impression>();
                }
                mImpressions.add(impression);
            }
            return this;
        }

        /**
         * Set custom options to be associated with the event. Should be used with the {@link Product#CHECKOUT} and {@link Product#CHECKOUT_OPTION} actions.
         *
         *
         * @param options a String describing this checkout step
         * @return returns this Builder for easy method chaining.
         */
        public Builder checkoutOptions(String options) {
            mCheckoutOptions = options;
            return this;
        }

        /**
         * Create the CommerceEvent. This method should be called when all of your desired datapoints have been added.
         *
         * When the SDK is in {@link com.mparticle.MParticle.Environment#Development} mode, this method will throw an {@link IllegalStateException} if you have created an invalid CommerceEvent, such as by combining
         * {@link Product} objects with {@link Promotion} objects. When in {@link com.mparticle.MParticle.Environment#Production} mode, errors will be logged to the console.
         *
         * @return returns the resulting immutable CommerceEvent to be logged
         *
         * @see <code><a href="../MParticle.html#logEvent(com.mparticle.commerce.CommerceEvent)">MParticle.logEvent(com.mparticle.commerce.CommerceEvent)</a></code>
         */
        public CommerceEvent build() {
            return new CommerceEvent(this);
        }

        /**
         * Set the list name with the Products of the CommerceEvent. This value should be used with the {@link Product#DETAIL} and {@link Product#CLICK} actions.
         *
         * @param listName a String name identifying the product list
         * @return returns this Builder for easy method chaining.
         */
        public Builder productListName(String listName) {
            mProductListName = listName;
            return this;
        }

        /**
         * Set the list source name with the Products of the CommerceEvent. This value should be used with the {@link Product#DETAIL} and {@link Product#CLICK} actions.
         *
         * @param listSource a String name identifying the product's list source
         * @return returns this Builder for easy method chaining.
         */
        public Builder productListSource(String listSource) {
            mProductListSource = listSource;
            return this;
        }

        /**
         * Overwrite the Products associated with the CommerceEvent. This should only be used with {@link Product}-based CommerceEvents created with <code><a href="CommerceEvent.Builder.html#CommerceEvent.Builder(java.lang.String,%20com.mparticle.commerce.Product)">Builder(java.lang.String, com.mparticle.commerce.Product)</a></code>
         *
         * @param products the List of products to associate with the CommerceEvent
         * @return returns this Builder for easy method chaining.
         */
        public Builder products(List<Product> products) {
            productList = products;
            return this;
        }

        /**
         * Overwrite the Impressions associated with the CommerceEvent. This should only be used with {@link Impression}-based CommerceEvents created with <code><a href="CommerceEvent.Builder.html#CommerceEvent.Builder(com.mparticle.commerce.Impression)">Builder(com.mparticle.commerce.Impression)</a></code>
         *
         * @param impressions the List of products to associate with the CommerceEvent
         * @return returns this Builder for easy method chaining.
         */
        public Builder impressions(List<Impression> impressions) {
            mImpressions = impressions;
            return this;
        }

        /**
         * Overwrite the Promotions associated with the CommerceEvent. This should only be used with {@link Promotion}-based CommerceEvents created with <code><a href="CommerceEvent.Builder.html#CommerceEvent.Builder(java.lang.String,%20com.mparticle.commerce.Promotion)">Builder(java.lang.String, com.mparticle.commerce.Promotion)</a></code>
         *
         * @param promotions the List of products to associate with the CommerceEvent
         * @return returns this Builder for easy method chaining.
         */
        public Builder promotions(List<Promotion> promotions) {
            promotionList = promotions;
            return this;
        }

        /**
         * Private API used internally by the SDK.
         *
         */
        public Builder internalEventName(String eventName) {
            mEventName = eventName;
            return this;
        }
    }
}
