package github.sachin2dehury.owlmail.repository

import android.util.Base64
import androidx.lifecycle.asLiveData
import github.sachin2dehury.owlmail.api.calls.BasicAuthInterceptor
import github.sachin2dehury.owlmail.api.calls.MailApi
import github.sachin2dehury.owlmail.api.data.Mail
import github.sachin2dehury.owlmail.api.data.Mails
import github.sachin2dehury.owlmail.api.database.MailDao
import github.sachin2dehury.owlmail.others.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withContext
import okhttp3.ResponseBody
import org.jsoup.Jsoup
import retrofit2.Response

@Suppress("BlockingMethodInNonBlockingContext")
class Repository(
    private val basicAuthInterceptor: BasicAuthInterceptor,
    private val dataStore: DataStoreExt,
    private val internetChecker: InternetChecker,
    private val mailApi: MailApi,
    private val mailDao: MailDao,
    private val networkBoundResource: NetworkBoundResource,
) {

    fun getParsedMailItem(
        id: String,
    ): Flow<Resource<Mail>> {
        return networkBoundResource.makeNetworkRequest(
            query = {
                mailDao.getMailItem(id)
            },
            fetch = {
                mailApi.getMailItemBody(Constants.MESSAGE_URL, id)
            },
            saveFetchResult = { response ->
                updateMailBody(response, id)
            },
            shouldFetch = {
                internetChecker.isInternetConnected()
            },
        )
    }

    fun getMails(request: String, search: String): Flow<Resource<List<Mail>>> {
        val box = getBox(request)
        return networkBoundResource.makeNetworkRequest(
            query = {
                mailDao.getMails(box)
            },
            fetch = {
                mailApi.getMails(request, search)
            },
            saveFetchResult = { response ->
                insertMails(response)
            },
            shouldFetch = {
                internetChecker.isInternetConnected()
            },
        )
    }

    suspend fun login(credential: String) = withContext(Dispatchers.IO) {
        basicAuthInterceptor.credential = credential
        try {
            val response =
                mailApi.login(Constants.UPDATE_QUERY + System.currentTimeMillis())
            if (response.isSuccessful && response.code() == 200) {
                saveLogInCredential()
                Resource.success(response.body()?.mails)
            } else {
                Resource.error(response.message(), null)
            }
        } catch (e: Exception) {
            Resource.error(
                e.message
                    ?: "Couldn't connect to the servers. Check your internet connection", null
            )
        }
    }

    fun isLoggedIn() = flow {
        var result = false
        dataStore.apply {
            readCredential(Constants.KEY_TOKEN)?.let { token ->
                if (token != Constants.NO_TOKEN) {
                    debugLog(System.currentTimeMillis().toString())
                    emit(true)
                    basicAuthInterceptor.token = token
                    result = true
                }
            }
            readCredential(Constants.KEY_CREDENTIAL)?.let { credential ->
                if (credential != Constants.NO_CREDENTIAL) {
                    emit(true)
                    basicAuthInterceptor.credential = credential
                    result = true
                }
            }
        }
        emit(result)
    }.asLiveData().value

    suspend fun logout() {
        basicAuthInterceptor.credential = Constants.NO_CREDENTIAL
        basicAuthInterceptor.token = Constants.NO_TOKEN
        saveLogInCredential()
        saveLastSync(Constants.INBOX_URL, Constants.NO_LAST_SYNC)
        saveLastSync(Constants.SENT_URL, Constants.NO_LAST_SYNC)
        saveLastSync(Constants.DRAFT_URL, Constants.NO_LAST_SYNC)
        saveLastSync(Constants.JUNK_URL, Constants.NO_LAST_SYNC)
        saveLastSync(Constants.TRASH_URL, Constants.NO_LAST_SYNC)
        mailDao.deleteAllMails()
    }

    fun readLastSync(request: String) = dataStore.readLastSync(Constants.KEY_LAST_SYNC + request)

    suspend fun saveLastSync(request: String, lastSync: Long) {
        if (internetChecker.isInternetConnected()) {
            dataStore.saveLastSync(Constants.KEY_LAST_SYNC + request, lastSync)
        }
    }

    suspend fun saveThemeState(state: String) {
        dataStore.saveCredential(Constants.KEY_THEME, state)
    }

    fun readThemeState() = dataStore.readCredential(Constants.KEY_THEME) ?: Constants.DARK_THEME

    private suspend fun saveLogInCredential() {
        dataStore.saveCredential(Constants.KEY_CREDENTIAL, basicAuthInterceptor.credential)
        dataStore.saveCredential(Constants.KEY_TOKEN, basicAuthInterceptor.token)
    }

    private suspend fun insertMails(response: Response<Mails>) {
        response.body()?.mails?.let { mails ->
            mails.forEach { mail ->
                mailDao.insertMail(mail)
            }
        }
    }

    private suspend fun updateMailBody(
        response: ResponseBody,
        id: String,
    ) {
        val token = getToken().substringAfter('=')
        val parsedMail = Jsoup.parse(response.string())
        response.close()
        parsedMail.getElementsByClass("MsgHdr").remove()
        parsedMail.removeClass("MsgBody")
        parsedMail.removeClass("Msg")
        parsedMail.removeClass("ZhAppContent")
        var body = parsedMail.toString()
        if (body.contains("auth=co", true)) {
            body = body.replace(
                "auth=co",
                "auth=qp&amp;zauthtoken=$token"
            )
        }
        mailDao.updateMail(body, id)
    }

    private fun getBox(request: String) = when (request) {
        Constants.INBOX_URL -> 2
        Constants.TRASH_URL -> 3
        Constants.JUNK_URL -> 4
        Constants.SENT_URL -> 5
        Constants.DRAFT_URL -> 6
        else -> 0
    }.toString()

    fun getToken() = basicAuthInterceptor.token

    fun getUser() = try {
        Base64.decode(basicAuthInterceptor.credential, Base64.DEFAULT).toString()
    } catch (e: Exception) {
        e.message
    }
}