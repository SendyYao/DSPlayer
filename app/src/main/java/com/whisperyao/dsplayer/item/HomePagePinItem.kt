package com.whisperyao.dsplayer.item

import android.os.Bundle
import com.whisperyao.dsplayer.vos.api.pin.PinItemVo

data class HomePagePinItem(
    var type: String?,
    var id: String?,
    var title: String?,
    var criteria: HashMap<String, String>,
    var marked: Boolean = false
) {

    companion object {

        fun fromPinItemVo(pinItemVo: PinItemVo): HomePagePinItem {
            return HomePagePinItem(
                type = pinItemVo.type,
                id = pinItemVo.id,
                title = pinItemVo.name,
                criteria = pinItemVo.criteria
            )
        }

        fun fromBundle(bundle: Bundle): HomePagePinItem {
            return HomePagePinItem(
                type = bundle.getString("type") ?: "",
                id = bundle.getString("id") ?: "",
                title = bundle.getString("title") ?: "",
                criteria = bundle.getSerializable("criteria") as HashMap<String, String>
            )
        }

        fun generateByPinItemVo(pinItemVo: PinItemVo): HomePagePinItem {
            return HomePagePinItem(pinItemVo.type, pinItemVo.id, pinItemVo.name, pinItemVo.criteria)
        }
    }

    fun toBundle(): Bundle {
        return Bundle().apply {
            putString("id", id)
            putString("title", title)
            putString("type", type)
            putSerializable("criteria", criteria)
        }
    }

}