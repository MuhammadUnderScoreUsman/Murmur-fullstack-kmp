package com.example.chatappkross.auth.data.common

import com.example.chatappkross.auth.domain.model.UserRecord
import com.example.chatappkross.model.MessageDto
import com.example.chatappkross.model.UserDto

fun UserRecord.toDto() = UserDto(id = id, username = username)
