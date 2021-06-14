package me.zakharov.events

import com.badlogic.gdx.scenes.scene2d.Event

class PauseEvent(public val pause: Boolean = true): Event() {
}