package com.sg.exoplayerlearning.models

data class PlayerAction(
    val actionType: ActionType,
    val data: Any? =  null,
)