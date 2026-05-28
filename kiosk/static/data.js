// ── 메뉴 데이터 ──────────────────────────────────────
const menuData = {
  "추천메뉴": [
    { name: "아메리카노", price: 3000, image: "images/1.png", type: "coffee" },
    { name: "슈크림라떼", price: 3900, image: "images/5.png", type: "coffee", badge: "new" },
    { name: "딸기쿠키프라페", price: 4500, image: "images/12.png", type: "drink" },
    { name: "요거트스무디", price: 4200, image: "images/14.png", type: "drink" },
    { name: "밀크티", price: 4500, image: "images/17.png", type: "noncoffee", badge: "new" },
    { name: "소금빵", price: 3500, image: "images/24.png", type: "dessert" },
    { name: "티라미수케이크", price: 7500, image: "images/25.png", type: "dessert" }
  ],
  "커피": [
    { name: "아메리카노", price: 3000, image: "images/1.png", type: "coffee" },
    { name: "카페라떼", price: 3500, image: "images/2.png", type: "coffee" },
    { name: "바닐라라떼", price: 3900, image: "images/3.png", type: "coffee" },
    { name: "연유라떼", price: 3900, image: "images/4.png", type: "coffee" },
    { name: "슈크림라떼", price: 3900, image: "images/5.png", type: "coffee", badge: "new" },
    { name: "아인슈페너", price: 4200, image: "images/6.png", type: "coffee" },
    { name: "콜드브루", price: 4000, image: "images/7.png", type: "coffee" }
  ],
  "에이드": [
    { name: "유자에이드", price: 4200, image: "images/8.png", type: "drink" },
    { name: "청포도에이드", price: 4000, image: "images/9.png", type: "drink" },
    { name: "자몽에이드", price: 4000, image: "images/10.png", type: "drink" },
    { name: "레몬에이드", price: 3800, image: "images/11.png", type: "drink" }
  ],
  "프라페/스무디": [
    { name: "딸기쿠키프라페", price: 4500, image: "images/12.png", type: "drink" },
    { name: "오레오프라페", price: 4300, image: "images/13.png", type: "drink" },
    { name: "요거트스무디", price: 4200, image: "images/14.png", type: "drink" },
    { name: "블루베리스무디", price: 4800, image: "images/15.png", type: "drink" },
    { name: "망고스무디", price: 4800, image: "images/16.png", type: "drink" }
  ],
  "논커피": [
    { name: "밀크티", price: 4500, image: "images/17.png", type: "noncoffee", badge: "new" },
    { name: "녹차라떼", price: 4700, image: "images/18.png", type: "noncoffee" },
    { name: "초코라떼", price: 3900, image: "images/19.png", type: "noncoffee" },
    { name: "토피넛라떼", price: 4000, image: "images/20.png", type: "noncoffee" },
    { name: "딸기라떼", price: 4200, image: "images/21.png", type: "noncoffee" },
    { name: "복숭아아이스티", price: 3500, image: "images/22.png", type: "noncoffee" }
  ],
  "디저트": [
    { name: "치아바타", price: 4200, image: "images/23.png", type: "dessert" },
    { name: "소금빵", price: 3500, image: "images/24.png", type: "dessert" },
    { name: "티라미수케이크", price: 7500, image: "images/25.png", type: "dessert" },
    { name: "치즈케이크", price: 7500, image: "images/26.png", type: "dessert" },
    { name: "허니브레드", price: 6000, image: "images/27.png", type: "dessert" }
  ]
};

// ── 옵션 설정 ─────────────────────────────────────────
const optionConfig = {
  "아메리카노": {
    groups: [
      { title: "온도 선택", type: "single", required: true, options: [{ label: "ICE", value: "ICE", price: 0 }, { label: "HOT", value: "HOT", price: 0 }] },
      { title: "텀블러 옵션", type: "multi", options: [{ label: "텀블러에 담기", value: "텀블러", price: 0 }] },
      { title: "샷 선택", type: "single", options: [{ label: "연하게 +0", value: "연하게", price: 0 }, { label: "샷추가 +600", value: "샷추가", price: 600 }, { label: "2샷추가 +1200", value: "2샷추가", price: 1200 }] },
      { title: "추가 선택", type: "multi", options: [{ label: "바닐라시럽추가 +700", value: "바닐라시럽추가", price: 700 }, { label: "헤이즐넛시럽추가 +700", value: "헤이즐넛시럽추가", price: 700 }] }
    ]
  },
  "카페라떼": {
    groups: [
      { title: "온도 선택", type: "single", required: true, options: [{ label: "ICE", value: "ICE", price: 0 }, { label: "HOT", value: "HOT", price: 0 }] },
      { title: "텀블러 옵션", type: "multi", options: [{ label: "텀블러에 담기", value: "텀블러", price: 0 }] },
      { title: "샷 선택", type: "single", options: [{ label: "연하게 +0", value: "연하게", price: 0 }, { label: "샷추가 +600", value: "샷추가", price: 600 }, { label: "2샷추가 +1200", value: "2샷추가", price: 1200 }] },
      { title: "추가 선택", type: "multi", options: [{ label: "바닐라시럽추가 +700", value: "바닐라시럽추가", price: 700 }, { label: "헤이즐넛시럽추가 +700", value: "헤이즐넛시럽추가", price: 700 }] }
    ]
  }
};

Object.assign(optionConfig, {
  "바닐라라떼": optionConfig["카페라떼"],
  "연유라떼": { groups: [ { title: "온도 선택", type: "single", required: true, options: [{ label: "ICE", value: "ICE", price: 0 }, { label: "HOT", value: "HOT", price: 0 }] }, { title: "텀블러 옵션", type: "multi", options: [{ label: "텀블러에 담기", value: "텀블러", price: 0 }] }, { title: "샷 선택", type: "single", options: [{ label: "연하게 +0", value: "연하게", price: 0 }, { label: "샷추가 +600", value: "샷추가", price: 600 }, { label: "2샷추가 +1200", value: "2샷추가", price: 1200 }] }, { title: "추가 선택", type: "multi", options: [{ label: "바닐라시럽추가 +700", value: "바닐라시럽추가", price: 700 }, { label: "헤이즐넛시럽추가 +700", value: "헤이즐넛시럽추가", price: 700 }, { label: "연유 +500", value: "연유", price: 500 }] } ] },
  "슈크림라떼": { groups: [ { title: "온도 선택", type: "single", required: true, options: [{ label: "ICE", value: "ICE", price: 0 }, { label: "HOT", value: "HOT", price: 0 }] }, { title: "텀블러 옵션", type: "multi", options: [{ label: "텀블러에 담기", value: "텀블러", price: 0 }] }, { title: "추가 선택", type: "multi", options: [{ label: "바닐라시럽추가 +700", value: "바닐라시럽추가", price: 700 }, { label: "헤이즐넛시럽추가 +700", value: "헤이즐넛시럽추가", price: 700 }, { label: "휘핑크림추가 +500", value: "휘핑크림추가", price: 500 }] } ] },
  "초코라떼": { groups: [ { title: "온도 선택", type: "single", required: true, options: [{ label: "ICE", value: "ICE", price: 0 }, { label: "HOT", value: "HOT", price: 0 }] }, { title: "텀블러 옵션", type: "multi", options: [{ label: "텀블러에 담기", value: "텀블러", price: 0 }] }, { title: "추가 선택", type: "multi", options: [{ label: "바닐라시럽추가 +700", value: "바닐라시럽추가", price: 700 }, { label: "헤이즐넛시럽추가 +700", value: "헤이즐넛시럽추가", price: 700 }, { label: "휘핑크림추가 +500", value: "휘핑크림추가", price: 500 }] } ] },
  "토피넛라떼": { groups: [ { title: "온도 선택", type: "single", required: true, options: [{ label: "ICE", value: "ICE", price: 0 }, { label: "HOT", value: "HOT", price: 0 }] }, { title: "텀블러 옵션", type: "multi", options: [{ label: "텀블러에 담기", value: "텀블러", price: 0 }] }, { title: "추가 선택", type: "multi", options: [{ label: "바닐라시럽추가 +700", value: "바닐라시럽추가", price: 700 }, { label: "헤이즐넛시럽추가 +700", value: "헤이즐넛시럽추가", price: 700 }, { label: "휘핑크림추가 +500", value: "휘핑크림추가", price: 500 }] } ] },
  "딸기라떼": { groups: [ { title: "텀블러 옵션", type: "multi", options: [{ label: "텀블러에 담기", value: "텀블러", price: 0 }] }, { title: "추가 선택", type: "multi", options: [{ label: "바닐라시럽추가 +700", value: "바닐라시럽추가", price: 700 }, { label: "헤이즐넛시럽추가 +700", value: "헤이즐넛시럽추가", price: 700 }, { label: "휘핑크림추가 +500", value: "휘핑크림추가", price: 500 }] } ] },
  "녹차라떼": { groups: [ { title: "온도 선택", type: "single", required: true, options: [{ label: "ICE", value: "ICE", price: 0 }, { label: "HOT", value: "HOT", price: 0 }] }, { title: "텀블러 옵션", type: "multi", options: [{ label: "텀블러에 담기", value: "텀블러", price: 0 }] }, { title: "추가 선택", type: "multi", options: [{ label: "바닐라시럽추가 +700", value: "바닐라시럽추가", price: 700 }, { label: "헤이즐넛시럽추가 +700", value: "헤이즐넛시럽추가", price: 700 }, { label: "휘핑크림추가 +500", value: "휘핑크림추가", price: 500 }] } ] },
  "딸기쿠키프라페": { groups: [ { title: "텀블러 옵션", type: "multi", options: [{ label: "텀블러에 담기", value: "텀블러", price: 0 }] }, { title: "추가 선택", type: "multi", options: [{ label: "바닐라시럽추가 +700", value: "바닐라시럽추가", price: 700 }, { label: "헤이즐넛시럽추가 +700", value: "헤이즐넛시럽추가", price: 700 }, { label: "휘핑크림추가 +500", value: "휘핑크림추가", price: 500 }] } ] },
  "오레오프라페": { groups: [ { title: "텀블러 옵션", type: "multi", options: [{ label: "텀블러에 담기", value: "텀블러", price: 0 }] }, { title: "추가 선택", type: "multi", options: [{ label: "바닐라시럽추가 +700", value: "바닐라시럽추가", price: 700 }, { label: "헤이즐넛시럽추가 +700", value: "헤이즐넛시럽추가", price: 700 }, { label: "휘핑크림추가 +500", value: "휘핑크림추가", price: 500 }] } ] },
  "복숭아아이스티": { groups: [ { title: "텀블러 옵션", type: "multi", options: [{ label: "텀블러에 담기", value: "텀블러", price: 0 }] }, { title: "샷 선택", type: "single", options: [{ label: "연하게 +0", value: "연하게", price: 0 }, { label: "샷추가 +600", value: "샷추가", price: 600 }, { label: "2샷추가 +1200", value: "2샷추가", price: 1200 }] } ] },
  "아인슈페너": { groups: [ { title: "텀블러 옵션", type: "multi", options: [{ label: "텀블러에 담기", value: "텀블러", price: 0 }] }, { title: "샷 선택", type: "single", options: [{ label: "연하게 +0", value: "연하게", price: 0 }, { label: "샷추가 +600", value: "샷추가", price: 600 }, { label: "2샷추가 +1200", value: "2샷추가", price: 1200 }] } ] },
  "콜드브루": { groups: [ { title: "온도 선택", type: "single", required: true, options: [{ label: "ICE", value: "ICE", price: 0 }, { label: "HOT", value: "HOT", price: 0 }] }, { title: "텀블러 옵션", type: "multi", options: [{ label: "텀블러에 담기", value: "텀블러", price: 0 }] }, { title: "샷 선택", type: "single", options: [{ label: "연하게 +0", value: "연하게", price: 0 }, { label: "샷추가 +600", value: "샷추가", price: 600 }, { label: "2샷추가 +1200", value: "2샷추가", price: 1200 }] } ] },
  "밀크티": { groups: [ { title: "텀블러 옵션", type: "multi", options: [{ label: "텀블러에 담기", value: "텀블러", price: 0 }] }, { title: "추가 선택", type: "multi", options: [{ label: "펄추가 +700", value: "펄추가", price: 700 }] } ] }
});
