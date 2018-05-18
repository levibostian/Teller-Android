package com.levibostian.teller.datastate.listener

import com.levibostian.teller.repository.OnlineRepository
import java.util.*

interface OnlineDataStateListener<in DATA>: OnlineDataStateFetchingListener, OnlineDataStateFirstFetchListener, OnlineDataStateCacheListener<DATA>