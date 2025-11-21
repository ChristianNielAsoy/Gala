package com.example.gala.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.gala.data.database.entities.Member
import com.example.gala.data.repository.GalaRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class PeopleViewModel(private val repo: GalaRepository) : ViewModel() {

    val members: StateFlow<List<Member>> =
        repo.getAllMembers().stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            emptyList()
        )

    fun addMember(member: Member) {
        viewModelScope.launch {
            repo.insertMember(member)
        }
    }
}
