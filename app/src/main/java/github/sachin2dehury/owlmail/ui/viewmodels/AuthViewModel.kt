package github.sachin2dehury.owlmail.ui.viewmodels

import androidx.hilt.lifecycle.ViewModelInject
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import github.sachin2dehury.owlmail.api.data.Mail
import github.sachin2dehury.owlmail.others.Resource
import github.sachin2dehury.owlmail.repository.Repository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class AuthViewModel @ViewModelInject constructor(
    private val repository: Repository
) : ViewModel() {

    val isLoggedIn = repository.isLoggedIn()

    private val _loginStatus = MutableLiveData<Resource<List<Mail>>>()
    val loginStatus: LiveData<Resource<List<Mail>>> = _loginStatus

    fun login(credential: String) {
        _loginStatus.postValue(Resource.loading(null))
        if (credential.isEmpty()) {
            _loginStatus.postValue(Resource.error("Please fill out all the fields", null))
            return
        }
        CoroutineScope(Dispatchers.IO).launch {
            val result = repository.login(credential)
            _loginStatus.postValue(result)
        }
    }
}