package me.fzzyhmstrs.amethyst_core.coding_util

import java.util.function.Consumer

data class Dustbin<T>(private val consumer: Consumer<T>, private val noDust: T, private var dust: T = noDust){
    private var dirty: Boolean = false
    fun markDirty(newDust: T){
        if (isDirty()) {
            consumer.accept(dust)
        }
        dust = newDust
        checkDirty()
    }
    fun isDirty(): Boolean{
        return dirty
    }
    private fun checkDirty(){
        dirty = dust != noDust
    }
    fun clean(){
        consumer.accept(dust)
        dust = noDust
        checkDirty()
    }
}
