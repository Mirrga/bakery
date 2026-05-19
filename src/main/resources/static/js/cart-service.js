// Функция добавления товара в корзину
function addToCart(productId, quantity = 1) {
    const url = `/cart/add?productId=${productId}&quantity=${quantity}`;
    
    // Находим кнопку, по которой кликнули (для анимации)
    const button = document.querySelector(`button[data-id="${productId}"]`) || 
                   document.querySelector(`button[onclick*="addToCart(${productId})"]`);
    
    if (button) {
        // Визуальный эффект нажатия
        const originalText = button.innerHTML;
        button.innerHTML = '<i class="fas fa-check"></i> Добавлено';
        button.classList.add('btn-success');
        button.classList.remove('btn-primary', 'btn-outline-dark');
        button.disabled = true;

        setTimeout(() => {
            button.innerHTML = originalText;
            button.classList.remove('btn-success');
            button.classList.add('btn-primary', 'btn-outline-dark');
            button.disabled = false;
        }, 1500);
    }

    // Отправка запроса на сервер
    fetch(url, {
        method: 'POST',
        headers: {
            'Content-Type': 'application/x-www-form-urlencoded'
        }
    })
    .then(response => {
        if (!response.ok) {
            throw new Error('Ошибка сети или сервера');
        }
        return response.json();
    })
    .then(data => {
        if (data.success) {
            console.log('Товар добавлен:', data.message);
            
            // 1. Обновляем счетчик в хедере
            if (typeof updateCartBadge === 'function') {
                updateCartBadge(data.totalItems);
            }

            // 2. Показываем уведомление (Toast)
            showToast(data.message, 'success');
        } else {
            showToast(data.message || 'Ошибка при добавлении', 'error');
        }
    })
    .catch(error => {
        console.error('Ошибка:', error);
        showToast('Произошла ошибка при добавлении товара', 'error');
        
        // Возвращаем кнопку в исходное состояние при ошибке
        if (button) {
            button.innerHTML = '<i class="fas fa-plus"></i> В корзину';
            button.classList.remove('btn-success');
            button.classList.add('btn-primary', 'btn-outline-dark');
            button.disabled = false;
        }
    });
}

// Функция обновления счетчика корзины (вызывается из контроллера или после добавления)
function updateCartBadge(count) {
    const badge = document.getElementById('cart-badge');
    if (badge) {
        badge.innerText = count;
        if (count > 0) {
            badge.style.display = 'inline-block';
        } else {
            badge.style.display = 'none';
        }
    }
}

// Функция показа всплывающего уведомления (Toast)
function showToast(message, type = 'success') {
    // Создаем элемент Toast, если его нет
    let toastContainer = document.getElementById('toast-container');
    if (!toastContainer) {
        toastContainer = document.createElement('div');
        toastContainer.id = 'toast-container';
        toastContainer.className = 'position-fixed bottom-0 end-0 p-3';
        toastContainer.style.zIndex = '11';
        document.body.appendChild(toastContainer);
    }

    const toastId = 'toast-' + Date.now();
    const bgClass = type === 'success' ? 'bg-success' : 'bg-danger';
    const icon = type === 'success' ? '<i class="fas fa-check-circle me-2"></i>' : '<i class="fas fa-exclamation-circle me-2"></i>';

    const toastHtml = `
        <div id="${toastId}" class="toast align-items-center text-white ${bgClass} border-0" role="alert" aria-live="assertive" aria-atomic="true">
            <div class="d-flex">
                <div class="toast-body">
                    ${icon}${message}
                </div>
                <button type="button" class="btn-close btn-close-white me-2 m-auto" data-bs-dismiss="toast" aria-label="Close"></button>
            </div>
        </div>
    `;

    toastContainer.insertAdjacentHTML('beforeend', toastHtml);

    // Инициализируем и показываем Toast через Bootstrap
    const toastElement = document.getElementById(toastId);
    const toast = new bootstrap.Toast(toastElement, { delay: 3000 });
    toast.show();

    // Удаляем элемент из DOM после скрытия
    toastElement.addEventListener('hidden.bs.toast', function () {
        toastElement.remove();
    });
}

// Функции для страницы корзины (удаление, очистка)
function removeCartItem(itemId) {
    if(confirm('Вы уверены, что хотите удалить этот товар?')) {
        fetch(`/cart/remove/${itemId}`, { method: 'POST' })
            .then(res => res.json())
            .then(data => {
                if(data.success) {
                    location.reload(); // Перезагружаем страницу корзины
                } else {
                    alert('Ошибка: ' + data.message);
                }
            });
    }
}

function clearCart() {
    if(confirm('Очистить всю корзину?')) {
        fetch('/cart/clear', { method: 'POST' })
            .then(res => res.json())
            .then(data => {
                if(data.success) {
                    location.reload();
                }
            });
    }
}

function updateCartItem(itemId, newQuantity) {
    if (newQuantity < 1) return;
    fetch(`/cart/update?itemId=${itemId}&quantity=${newQuantity}`, { method: 'POST' })
        .then(res => res.json())
        .then(data => {
            if(data.success) {
                location.reload();
            }
        });
}