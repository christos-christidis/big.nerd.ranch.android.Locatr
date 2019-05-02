package com.bignerdranch.locatr;

class GalleryItem {

    private String mCaption;
    private String mUrl;
    private double mLat;

    double getLat() {
        return mLat;
    }

    void setLat(double lat) {
        mLat = lat;
    }

    double getLon() {
        return mLon;
    }

    void setLon(double lon) {
        mLon = lon;
    }

    private double mLon;

    void setCaption(String caption) {
        mCaption = caption;
    }

    String getUrl() {
        return mUrl;
    }

    void setUrl(String url) {
        mUrl = url;
    }

    @SuppressWarnings("NullableProblems")
    @Override
    public String toString() {
        return mCaption;
    }
}
