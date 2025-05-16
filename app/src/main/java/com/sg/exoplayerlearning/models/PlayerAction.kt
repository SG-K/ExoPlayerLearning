package com.sg.exoplayerlearning.models

//import com.sg.exoplayerlearning.ActionType

data class PlayerAction(
    val actionType: ActionType,
    val data: Any? =  null,
)