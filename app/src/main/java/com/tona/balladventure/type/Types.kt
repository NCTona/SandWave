package com.tona.balladventure.type

object Types {

    // Chế độ nhảy — mặc định
    val JUMP = GameType(
        name = "JUMP"
    )

    // Chế độ chuyển đổi — ví dụ đổi trọng lực, vật lý, hoặc cơ chế điều khiển
    val SWITCH = GameType(
        name = "SWITCH"
    )

    // Chế độ bay — ví dụ cho phép giữ để bay hoặc điều khiển liên tục
    val FLY = GameType(
        name = "FLY"
    )
}
