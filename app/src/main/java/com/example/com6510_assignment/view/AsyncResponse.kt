package com.example.currentplacedetailsonmap

import com.google.android.gms.maps.model.PolylineOptions

interface AsyncResponse {
    fun onDataReceivedSuccess(listData: PolylineOptions?, data: String?)
    fun onDataReceivedFailed()
}