package me.snipz.economy.api

interface ICurrency {
    fun name(): String
    fun global(): Boolean
}