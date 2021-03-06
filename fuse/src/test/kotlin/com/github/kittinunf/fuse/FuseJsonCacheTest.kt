package com.github.kittinunf.fuse

import com.github.kittinunf.fuse.core.Fuse
import com.github.kittinunf.fuse.core.fetch.get
import org.hamcrest.CoreMatchers.*
import org.json.JSONObject
import org.junit.Assert.assertThat
import org.junit.Before
import org.junit.Test
import java.io.FileNotFoundException
import java.net.URL
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executor
import org.hamcrest.CoreMatchers.`is` as isEqualTo

class FuseJsonCacheTest : BaseTestCase() {

    var hasSetUp = false

    @Before
    fun initialize() {
        if (!hasSetUp) {
            hasSetUp = true

            Fuse.init(tempDirString)
            Fuse.callbackExecutor = Executor { it.run() }
        }
    }

    @Test
    fun firstFetch() {
        val lock = CountDownLatch(1)
        val json = assetDir.resolve("sample_json.json")

        var value: JSONObject? = null
        var error: Exception? = null

        Fuse.jsonCache.get(json) { result ->
            val (v, e) = result
            value = v
            error = e
            lock.countDown()
        }

        lock.wait()

        assertThat(value, notNullValue())
        assertThat(value!!.getString("name"), isEqualTo("Product"))
        assertThat(error, nullValue())
    }

    @Test
    fun fetchFromNetworkSuccess() {
        val lock = CountDownLatch(1)
        val httpBin = URL("http://www.httpbin.org/get")

        var value: JSONObject? = null
        var error: Exception? = null

        Fuse.jsonCache.get(httpBin) { result, type ->
            val (v, e) = result
            value = v
            error = e
            lock.countDown()
        }

        lock.wait()
        assertThat(value, notNullValue())
        assertThat(value!!.getString("url"), isEqualTo("http://www.httpbin.org/get"))
        assertThat(error, nullValue())
    }

    @Test
    fun fetchFromNetworkFail() {
        val lock = CountDownLatch(1)
        val failedHttpBin = URL("http://www.httpbin.org/t")

        var value: JSONObject? = null
        var error: Exception? = null

        Fuse.jsonCache.get(failedHttpBin) { result, type ->
            val (v, e) = result
            value = v
            error = e
            lock.countDown()
        }

        lock.wait()
        assertThat(value, nullValue())
        assertThat(error, notNullValue())
        assertThat(error as? FileNotFoundException, isA(FileNotFoundException::class.java))
    }

}