package com.levibostian.teller.repository

/**
 * Data object that are the requirements to fetch, get cached data. It could contain a query term to search for data. It could contain a user id to fetch a user from an API.
 *
 * @property tag Unique tag that drives the behavior of a [OnlineDataSource]. If the [tag] is ever changed, [OnlineDataSource] will trigger a new data fetch so that you can get new data.
 */
interface GetDataRequirements {
    var tag: String
}