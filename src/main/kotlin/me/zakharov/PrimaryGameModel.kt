package me.zakharov
interface IState {
    fun accept()
}
interface IGameModel {
    var state: IState
}

class PrimaryGameModel: IGameModel {
    override var state: IState
        get() = TODO("Not yet implemented")
        set(value) {}
}
