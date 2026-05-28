// ── 전역 상태 ─────────────────────────────────────────
let orderType = "";
let currentItem = null;
let currentPrimary = "추천메뉴";
let cart = [];
let selectedOptions = {};

// ── 모달 ──────────────────────────────────────────────
function closeModal(id) {
  document.getElementById(id).style.display = 'none';
}

// ── 옵션 모달 ─────────────────────────────────────────
function resetOptionState() {
  selectedOptions = {};
  document.getElementById('option-extra-price').innerText = '0';
}

function getCurrentOptionConfig() {
  return optionConfig[currentItem.name] || { groups: [] };
}

function renderOptionSections() {
  const container = document.getElementById('option-sections');
  const config = getCurrentOptionConfig();
  container.innerHTML = '';

  if (!config.groups || config.groups.length === 0) {
    updateOptionExtraPrice();
    return;
  }

  config.groups.forEach((group, groupIndex) => {
    const box = document.createElement('div');
    box.className = 'option-box';

    const title = document.createElement('div');
    title.className = 'option-title';
    title.innerText = group.required ? `${group.title} (필수 선택)` : group.title;
    box.appendChild(title);

    const btnGroup = document.createElement('div');
    btnGroup.className = 'option-btn-group';

    group.options.forEach((option, optionIndex) => {
      const btn = document.createElement('button');
      btn.type = 'button';
      btn.className = 'option-select-btn';
      btn.innerText = option.label;
      btn.onclick = function() {
        toggleOption(groupIndex, optionIndex, btn, group.type);
      };
      btnGroup.appendChild(btn);
    });

    box.appendChild(btnGroup);
    container.appendChild(box);
  });

  updateOptionExtraPrice();
}

function toggleOption(groupIndex, optionIndex, buttonEl, groupType) {
  if (groupType === 'single') {
    selectedOptions[groupIndex] = optionIndex;
    const groupBox = buttonEl.parentElement;
    groupBox.querySelectorAll('.option-select-btn').forEach(btn => btn.classList.remove('active'));
    buttonEl.classList.add('active');
  } else {
    if (!selectedOptions[groupIndex]) selectedOptions[groupIndex] = [];
    const idx = selectedOptions[groupIndex].indexOf(optionIndex);
    if (idx > -1) {
      selectedOptions[groupIndex].splice(idx, 1);
      buttonEl.classList.remove('active');
    } else {
      selectedOptions[groupIndex].push(optionIndex);
      buttonEl.classList.add('active');
    }
  }
  updateOptionExtraPrice();
}

function updateOptionExtraPrice() {
  const config = getCurrentOptionConfig();
  let extra = 0;

  if (config.groups) {
    config.groups.forEach((group, groupIndex) => {
      const selected = selectedOptions[groupIndex];
      if (group.type === 'single') {
        if (selected !== undefined && group.options[selected]) {
          extra += group.options[selected].price;
        }
      } else {
        if (Array.isArray(selected)) {
          selected.forEach(optionIndex => {
            if (group.options[optionIndex]) {
              extra += group.options[optionIndex].price;
            }
          });
        }
      }
    });
  }

  document.getElementById('option-extra-price').innerText = extra.toLocaleString();
}

function openOptionModal(item) {
  currentItem = item;
  document.getElementById('option-title').innerText = item.name;
  resetOptionState();
  renderOptionSections();
  document.getElementById('option-modal').style.display = 'flex';
}

// ── 장바구니 ──────────────────────────────────────────
function addToCart() {
  let finalPrice = currentItem.price;
  const config = getCurrentOptionConfig();
  const optionParts = [];

  for (let i = 0; i < config.groups.length; i++) {
    const group = config.groups[i];
    const selected = selectedOptions[i];
    if (group.required && group.type === "single" && selected === undefined) {
      alert(`${group.title}을(를) 선택해주세요.`);
      return;
    }
  }

  if (config.groups) {
    config.groups.forEach((group, groupIndex) => {
      const selected = selectedOptions[groupIndex];
      if (group.type === 'single') {
        if (selected !== undefined && group.options[selected]) {
          finalPrice += group.options[selected].price;
          optionParts.push(group.options[selected].value);
        }
      } else {
        if (Array.isArray(selected)) {
          selected.forEach(optionIndex => {
            if (group.options[optionIndex]) {
              finalPrice += group.options[optionIndex].price;
              optionParts.push(group.options[optionIndex].value);
            }
          });
        }
      }
    });
  }

  const optionLabel = optionParts.join(", ");
  const existingItem = cart.find(item =>
    item.name === currentItem.name &&
    item.unitPrice === finalPrice &&
    item.optionLabel === optionLabel
  );

  if (existingItem) {
    existingItem.qty += 1;
  } else {
    cart.push({
      name: currentItem.name,
      unitPrice: finalPrice,
      qty: 1,
      optionLabel: optionLabel
    });
  }

  updateCartUI();
  closeModal('option-modal');
}

function updateCartUI() {
  const wrap = document.getElementById('cart-items-wrap');
  wrap.innerHTML = "";
  let totalPrice = 0;

  cart.forEach((item, index) => {
    totalPrice += item.unitPrice * item.qty;
    const cartDiv = document.createElement('div');
    cartDiv.className = 'cart-list';

    cartDiv.innerHTML = `
      <div style="display:flex; align-items:center; gap:8px; min-width:0; flex:1;">
        <button class="qty-btn" onclick="removeCartItem(${index})">❌</button>
        <div class="cart-item-info">
          <span style="font-size:14px; white-space:nowrap; overflow:hidden; text-overflow:ellipsis;">${item.name}</span>
          ${item.optionLabel ? `<span style="font-size:11px; color:#777; font-weight:normal;">${item.optionLabel}</span>` : ""}
        </div>
      </div>
      <div class="qty-control">
        <button class="qty-btn" onclick="decreaseQty(${index})">-</button>
        <span>${item.qty}</span>
        <button class="qty-btn" onclick="increaseQty(${index})">+</button>
        <span style="min-width: 60px; text-align: right;">${(item.unitPrice * item.qty).toLocaleString()}원</span>
      </div>
    `;
    wrap.appendChild(cartDiv);
  });

  document.getElementById('total-pay-price').innerText = totalPrice.toLocaleString();
}

function increaseQty(index) {
  cart[index].qty += 1;
  updateCartUI();
}

function decreaseQty(index) {
  cart[index].qty -= 1;
  if (cart[index].qty <= 0) cart.splice(index, 1);
  updateCartUI();
}

function removeCartItem(index) {
  cart.splice(index, 1);
  updateCartUI();
}

function clearCart() {
  cart = [];
  orderType = "";
  updateCartUI();
  closeModal('option-modal');
  closeModal('confirm-modal');
  closeModal('order-type-modal');
  closeModal('payment-modal');
}

// ── 결제 플로우 ───────────────────────────────────────
function renderConfirmCart() {
  const container = document.getElementById('confirm-cart-list');
  const totalEl = document.getElementById('confirm-total-price');
  container.innerHTML = '';
  let totalPrice = 0;

  cart.forEach(item => {
    const sumPrice = item.unitPrice * item.qty;
    totalPrice += sumPrice;

    const div = document.createElement('div');
    div.className = 'confirm-cart-item';
    div.innerHTML = `
      <div class="confirm-cart-top">
        <div class="confirm-cart-name">${item.name}</div>
        <div class="confirm-cart-qty">${item.qty}개</div>
      </div>
      ${item.optionLabel ? `<div class="confirm-cart-option">옵션: ${item.optionLabel}</div>` : `<div class="confirm-cart-option">옵션 없음</div>`}
      <div class="confirm-cart-price">${sumPrice.toLocaleString()}원</div>
    `;
    container.appendChild(div);
  });

  totalEl.innerText = `${totalPrice.toLocaleString()}원`;
}

function processPayment() {
  if (cart.length === 0) {
    alert("주문할 상품을 선택해주세요.");
    return;
  }
  renderConfirmCart();
  document.getElementById('confirm-modal').style.display = 'flex';
}

function goToOrderType() {
  closeModal('confirm-modal');
  document.getElementById('order-type-modal').style.display = 'flex';
}

function selectOrderType(type) {
  orderType = type;
  closeModal('order-type-modal');
  document.getElementById('payment-modal').style.display = 'flex';
}

function getCartTotalPrice() {
  return cart.reduce((sum, item) => sum + (item.unitPrice * item.qty), 0);
}

function payByCard() {
  const totalPrice = getCartTotalPrice();
  closeModal('payment-modal');
  alert(`[${orderType}] 주문이 완료되었습니다!\n결제금액: ${totalPrice.toLocaleString()}원`);
  clearCart();
}

function payByCoupon() {
  const totalPrice = getCartTotalPrice();
  closeModal('payment-modal');
  alert("바코드를 인식하세요.");
  alert(`[${orderType}] 모바일 쿠폰 결제가 완료되었습니다!\n결제금액: ${totalPrice.toLocaleString()}원`);
  clearCart();
}

// ── 메뉴 탭 / 렌더링 ──────────────────────────────────
function setPrimaryActive(categoryName) {
  document.querySelectorAll('.p-cat').forEach(tab => {
    tab.classList.toggle('active', tab.innerText === categoryName);
  });
}

function changePrimaryTab(clickedElement) {
  document.querySelectorAll('.p-cat').forEach(tab => tab.classList.remove('active'));
  clickedElement.classList.add('active');
  currentPrimary = clickedElement.innerText;
  renderMenu(currentPrimary);
}

function renderMenu(primaryCategory) {
  const grid = document.getElementById('menu-grid');
  grid.innerHTML = '';
  const items = menuData[primaryCategory] || [];

  items.forEach(item => {
    const menuDiv = document.createElement('div');
    menuDiv.className = 'menu-item';
    menuDiv.onclick = function() {
      openOptionModal(item);
    };

    menuDiv.innerHTML = `
      ${item.badge ? `<div class="badge ${item.badge}">${item.badge === 'new' ? '신메뉴' : '베스트'}</div>` : ""}
      <div class="menu-img-box">
        <img src="${item.image}" alt="${item.name}">
      </div>
      <div class="menu-name">${item.name}</div>
      <div class="menu-price">${item.price.toLocaleString()}원</div>
    `;
    grid.appendChild(menuDiv);
  });
}

// ── 스케일 조정 ───────────────────────────────────────
const BASE_W = 600;
const BASE_H = 960;

function scaleKiosk() {
  const kiosk = document.querySelector('.kiosk-container');
  const scaleX = window.innerWidth  / BASE_W;
  const scaleY = window.innerHeight / BASE_H;
  const scale  = Math.min(scaleX, scaleY);

  // top:50%, left:50% 기준으로 -50% 이동 후 scale 적용
  // → 화면 정중앙에 비율 유지하며 표시
  kiosk.style.transform = `translate(-50%, -50%) scale(${scale})`;
}

window.addEventListener('resize', scaleKiosk);

// ── 초기화 ────────────────────────────────────────────
window.onload = function() {
  currentPrimary = "추천메뉴";
  renderMenu(currentPrimary);
  updateCartUI();
  scaleKiosk();
};
