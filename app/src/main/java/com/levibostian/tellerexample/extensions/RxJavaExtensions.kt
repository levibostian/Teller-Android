package com.levibostian.tellerexample.extensions

import com.levibostian.teller.repository.TellerRepository
import com.levibostian.tellerexample.service.error.NetworkConnectionError
import com.levibostian.tellerexample.service.vo.Headers
import com.levibostian.tellerexample.service.vo.HttpSuccessfulResponse
import io.reactivex.Single
import retrofit2.Response

/**
 * If your HTTP request does not succeed (there is no status code and response from the server), Retrofit sends that to `Observable.onError()`. Instead of this, it would be easier if we could simply
 */
fun <T: Any, R: Any> Single<Response<T>>.transformMapSuccess(func: (HttpSuccessfulResponse<T>) -> TellerRepository.FetchResponse<R>): Single<TellerRepository.FetchResponse<R>> {
    return map { response ->
        func(HttpSuccessfulResponse(response.code(), response.body(), Headers(response.headers())))
    }.onErrorReturn {
        TellerRepository.FetchResponse.fail(NetworkConnectionError())
    }
}