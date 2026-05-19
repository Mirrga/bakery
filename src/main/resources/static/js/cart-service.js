$(document).ready(function() {
    console.log("🛒 Cart Service Initialized");

    // --- 1. Обработчики кнопок изменения количества (+/-) ---
    $(document).on('click', '.btn-qty-change', function(e) {
        e.preventDefault();
        let btn = $(this);
        let itemId = btn.data('item-id');
        let newQty = btn.data('quantity');

        if (newQty < 0) return; // Защита от отрицательных чисел

        updateCartItem(itemId, newQty);
    });

    // --- 2. Обработчик кнопки удаления товара ---
    $(document).on('click', '.btn-remove-item', function(e) {
        e.preventDefault();
        let btn = $(this);
        let itemId = btn.data('item-id');

        if(confirm('Вы уверены, что хотите удалить этот товар?')) {
            removeCartItem(itemId);
        }
    });

    // --- 3. Обработчик очистки всей корзины ---
    $('#btn-clear-cart').click(function(e) {
        e.preventDefault();
        if(confirm('Вы уверены, что хотите очистить всю корзину?')) {
            clearCart();
        }
    });

    // ================= ФУНКЦИИ API =================

    window.updateCartItem = function(itemId, quantity) {
        console.log(`Updating item ${itemId} to qty ${quantity}`);
        
        // Блокируем интерфейс на время запроса
        toggleLoading(true);

        $.post('/cart/update', {
            itemId: itemId,
            quantity: quantity
        })
        .done(function(response) {
            if (response.success) {
                updateRowVisuals(itemId, quantity, response.totalAmount);
                showToast("Количество обновлено", "success");
                
                // Если количество стало 0 (редкий кейс через API), удаляем строку
                if (quantity === 0) {
                    $(`#cart-item-row-${itemId}`).fadeOut(300, function() { $(this).remove(); checkEmpty(); });
                }
            } else {
                showToast(response.message || "Ошибка обновления", "danger");
            }
        })
        .fail(function(xhr) {
            console.error("Error updating cart", xhr);
            showToast("Не удалось обновить количество", "danger");
        })
        .always(function() {
            toggleLoading(false);
        });
    };

    window.removeCartItem = function(itemId) {
        console.log(`Removing item ${itemId}`);
        toggleLoading(true);

        $.post(`/cart/remove/${itemId}`)
        .done(function(response) {
            if (response.success) {
                $(`#cart-item-row-${itemId}`).fadeOut(300, function() {
                    $(this).remove();
                    checkEmpty();
                });
                
                // Обновляем итоговую сумму в футере
                $('#cart-total-amount').text(formatMoney(response.totalAmount));
                
                if (response.isEmpty) {
                    showToast("Корзина пуста", "info");
                } else {
                    showToast("Товар удален", "success");
                }
            } else {
                showToast(response.message || "Ошибка удаления", "danger");
            }
        })
        .fail(function(xhr) {
            console.error("Error removing item", xhr);
            showToast("Не удалось удалить товар", "danger");
        })
        .always(function() {
            toggleLoading(false);
        });
    };

    window.clearCart = function() {
        console.log("Clearing cart");
        toggleLoading(true);

        $.post('/cart/clear')
        .done(function(response) {
            if (response.success) {
                // Скрываем таблицу, показываем сообщение "пусто"
                $('.card').fadeOut(300, function() {
                    $('#cart-empty-message').fadeIn();
                });
                $('#cart-total-amount').text("0 ₽");
                showToast("Корзина очищена", "info");
            } else {
                showToast("Ошибка при очистке", "danger");
            }
        })
        .fail(function(xhr) {
            console.error("Error clearing cart", xhr);
            showToast("Не удалось очистить корзину", "danger");
        })
        .always(function() {
            toggleLoading(false);
        });
    };

    // ================= ВСПОМОГАТЕЛЬНЫЕ ФУНКЦИИ =================

    function updateRowVisuals(itemId, quantity, newTotalAmount) {
        let row = $(`#cart-item-row-${itemId}`);
        let price = parseFloat(row.data('price'));
        let subtotal = price * quantity;

        // Обновляем инпут
        row.find('.item-qty-input').val(quantity);
        
        // Обновляем сумму строки
        row.find('.item-subtotal').text(formatMoney(subtotal));
        
        // Обновляем общую сумму
        $('#cart-total-amount').text(formatMoney(newTotalAmount));
        
        // Обновляем data-атрибуты кнопок +/-
        row.find('.btn-qty-change').each(function() {
            let btn = $(this);
            let isPlus = btn.find('.fa-plus').length > 0;
            if (isPlus) {
                btn.data('quantity', quantity + 1);
            } else {
                btn.data('quantity', quantity - 1);
            }
        });
    }

    function checkEmpty() {
        if ($('.cart-item-row').length === 0) {
            $('.card').fadeOut(300, function() {
                $('#cart-empty-message').fadeIn();
                $('#cart-actions').hide(); // Скрываем кнопки действий
            });
        }
    }

    function formatMoney(amount) {
        // Простое форматирование числа с пробелами и рублем
        return new Intl.NumberFormat('ru-RU', { style: 'currency', currency: 'RUB' }).format(amount);
    }

    function toggleLoading(isLoading) {
        if (isLoading) {
            $('body').css('cursor', 'wait');
            $('.btn-qty-change, .btn-remove-item, #btn-clear-cart').prop('disabled', true);
        } else {
            $('body').css('cursor', 'default');
            $('.btn-qty-change, .btn-remove-item, #btn-clear-cart').prop('disabled', false);
        }
    }

    function showToast(message, type = 'info') {
        const toastEl = document.getElementById('cartToast');
        const toastBody = document.getElementById('cart-toast-message');
        const toastHeaderIcon = toastEl.querySelector('.toast-header i');
        
        toastBody.textContent = message;
        
        // Меняем цвет иконки в зависимости от типа
        toastHeaderIcon.className = ''; 
        if (type === 'success') toastHeaderIcon.classList.add('fas', 'fa-check-circle', 'me-2', 'text-success');
        else if (type === 'danger') toastHeaderIcon.classList.add('fas', 'fa-exclamation-triangle', 'me-2', 'text-danger');
        else toastHeaderIcon.classList.add('fas', 'fa-info-circle', 'me-2', 'text-primary');

        const toast = new bootstrap.Toast(toastEl);
        toast.show();
    }
});