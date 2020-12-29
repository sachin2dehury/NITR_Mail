package github.sachin2dehury.nitrmail.ui.viewmodels

import androidx.hilt.lifecycle.ViewModelInject
import androidx.lifecycle.*
import github.sachin2dehury.nitrmail.api.data.Mail
import github.sachin2dehury.nitrmail.others.Constants
import github.sachin2dehury.nitrmail.others.Event
import github.sachin2dehury.nitrmail.others.Resource
import github.sachin2dehury.nitrmail.repository.Repository
import kotlinx.coroutines.launch

class MailBoxViewModel @ViewModelInject constructor(
    private val repository: Repository
) : ViewModel() {

    private val _request = MutableLiveData(Constants.INBOX_URL)
    val request: LiveData<String> = _request

    var lastSync = Constants.NO_LAST_SYNC

    private val _forceUpdate = MutableLiveData(false)

    private val _mails = _forceUpdate.switchMap {
        repository.getMails(request.value!!, lastSync).asLiveData(viewModelScope.coroutineContext)
    }.switchMap {
        MutableLiveData(Event(it))
    }
    val mails: LiveData<Event<Resource<List<Mail>>>> = _mails

    fun saveLastSync(lastSync: Long) = viewModelScope.launch {
        repository.saveLastSync(request.value!!, lastSync)
    }

    fun readLastSync() = viewModelScope.launch { repository.readLastSync(request.value!!) }

    fun logOut() = viewModelScope.launch { repository.logOut() }

    fun syncAllMails() = _forceUpdate.postValue(true)

    fun setRequest(string: String) = _request.postValue(string)
}