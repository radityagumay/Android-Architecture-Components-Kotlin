/**
 * Copyright 2017 Erik Jhordan Rey.
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package erikjhordanrey.android_kotlin_devises.data.repository

import android.arch.lifecycle.LiveData
import android.arch.lifecycle.MutableLiveData
import android.arch.lifecycle.Transformations
import erikjhordanrey.android_kotlin_devises.data.remote.CurrencyResponse
import erikjhordanrey.android_kotlin_devises.data.remote.RemoteCurrencyDataSource
import erikjhordanrey.android_kotlin_devises.data.room.CurrencyEntity
import erikjhordanrey.android_kotlin_devises.data.room.RoomCurrencyDataSource
import erikjhordanrey.android_kotlin_devises.domain.AvailableExchange
import erikjhordanrey.android_kotlin_devises.domain.Currency
import io.reactivex.Completable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import javax.inject.Inject

class CurrencyRepository @Inject constructor(
    val roomCurrencyDataSource: RoomCurrencyDataSource,
    val remoteCurrencyDataSource: RemoteCurrencyDataSource
) : Repository {

  init {
    populateRoomDataSource(roomCurrencyDataSource)
  }

  override fun getCurrencyList(): LiveData<List<Currency>> {
    val roomCurrencyDao = roomCurrencyDataSource.currencyDao()
    return transform(roomCurrencyDao.getAllCurrencies())
  }

  override fun getAvailableExchange(currencies: String): LiveData<AvailableExchange> {
    var mutableAvailableExchange = MutableLiveData<AvailableExchange>()
    remoteCurrencyDataSource.requestAvailableExchange(currencies)
        .subscribeOn(Schedulers.io())
        .observeOn(AndroidSchedulers.mainThread())
        .subscribe { currency ->
          if (currency.isSuccess) {
            print("Is Success" + currency.isSuccess)
            mutableAvailableExchange = transform(currency)
          }else{
            print("onError")
          }
        }
    return mutableAvailableExchange
  }

  private fun transform(liveCurrencyEntity: LiveData<List<CurrencyEntity>>): LiveData<List<Currency>> {
    return Transformations.map(liveCurrencyEntity) { currencyEntities ->
      val currencyList = ArrayList<Currency>()
      currencyEntities.forEach {
        currencyList.add(Currency(it.countryCode, it.countryName))
      }
      currencyList
    }
  }

  private fun transform(currencyResponse: CurrencyResponse): MutableLiveData<AvailableExchange> {
    val mutableAvailableExchange = MutableLiveData<AvailableExchange>()
    mutableAvailableExchange.value = AvailableExchange(
        currencyResponse.currencyQuotes.quote1,
        currencyResponse.currencyQuotes.quote2
    )
    return mutableAvailableExchange
  }

  private fun populateRoomDataSource(roomCurrencyDataSource: RoomCurrencyDataSource) {
    val currencyDao = roomCurrencyDataSource.currencyDao()
    val currencyEntityList = RoomCurrencyDataSource.getAllCurrencies()
    Completable.create { currencyDao.insertAll(currencyEntityList) }
        .subscribeOn(Schedulers.io())
        .observeOn(AndroidSchedulers.mainThread())
        .subscribe { }
  }
}