package com.codespanner.app.model

data class MenuItem(val name: String, val price: Int, val category: String)

object MenuDatabase {

    val categories = listOf("추천메뉴", "커피", "에이드", "프라페/스무디", "논커피", "디저트")

    val all = listOf(
        // 커피
        MenuItem("아메리카노",    3000, "커피"),
        MenuItem("카페라떼",      3500, "커피"),
        MenuItem("바닐라라떼",    3900, "커피"),
        MenuItem("연유라떼",      3900, "커피"),
        MenuItem("슈크림라떼",    3900, "커피"),
        MenuItem("아인슈페너",    4200, "커피"),
        MenuItem("콜드브루",      4000, "커피"),
        // 에이드
        MenuItem("유자에이드",    4200, "에이드"),
        MenuItem("청포도에이드",  4000, "에이드"),
        MenuItem("자몽에이드",    4000, "에이드"),
        MenuItem("레몬에이드",    3800, "에이드"),
        // 프라페/스무디
        MenuItem("딸기쿠키프라페", 4500, "프라페/스무디"),
        MenuItem("오레오프라페",   4300, "프라페/스무디"),
        MenuItem("요거트스무디",   4200, "프라페/스무디"),
        MenuItem("블루베리스무디", 4800, "프라페/스무디"),
        MenuItem("망고스무디",     4800, "프라페/스무디"),
        // 논커피
        MenuItem("밀크티",        4500, "논커피"),
        MenuItem("녹차라떼",      4700, "논커피"),
        MenuItem("초코라떼",      3900, "논커피"),
        MenuItem("토피넛라떼",    4000, "논커피"),
        MenuItem("딸기라떼",      4200, "논커피"),
        MenuItem("복숭아아이스티", 3500, "논커피"),
        // 디저트
        MenuItem("치아바타",      4200, "디저트"),
        MenuItem("소금빵",        3500, "디저트"),
        MenuItem("티라미수케이크", 7500, "디저트"),
        MenuItem("치즈케이크",    7500, "디저트"),
        MenuItem("허니브레드",    6000, "디저트")
    )
}
