/**
 * Глобальная конфигурация приложения: jQuery AJAX setup, утилиты, обработчики ошибок.
 */
$(document).ready(function() {
    console.log("🚀 Bakery App Config Initialized");

    // 1. Настройка глобальных AJAX запросов
    $.ajaxSetup({
        beforeSend: function(xhr) {
            // Добавляем заголовок, чтобы сервер понял, что это AJAX
            xhr.setRequestHeader('X-Requested-With', 'XMLHttpRequest');
            
            // Если CSRF включен в будущем, этот блок подхватит токен из meta
            const token = $('meta[name="_csrf"]').attr('content');
            const header = $('meta[name="_csrf_header"]').attr('content');
            if (token && header) {
                xhr.setRequestHeader(header, token);
            }
        },
        error: function(xhr, status, error) {
            if (xhr.status === 403) {
                showToast("Ошибка доступа", "Сессия истекла. Обновите страницу.", "danger");
            } else if (xhr.status === 500) {
                showToast("Ошибка сервера", "Произошла внутренняя ошибка.", "danger");
                console.error("Server Error:", error);
            }
        }
    });

    // 2. Инициализация Bootstrap Toasts (если контейнер есть)
    window.showToast = function(title, message, type = 'primary') {
        const toastContainer = $('#toast-container');
        if (toastContainer.length === 0) {
            // Создаем контейнер, если его нет
            $('body').append('<div id="toast-container" class="position-fixed bottom-0 end-0 p-3" style="z-index: 11"></div>');
        }

        const toastId = 'toast-' + Date.now();
        const bgClass = type === 'danger' ? 'bg-danger' : (type === 'success' ? 'bg-success' : 'bg-primary');
        
        const toastHtml = `
            <div id="${toastId}" class="toast align-items-center text-white ${bgClass} border-0" role="alert" aria-live="assertive" aria-atomic="true">
                <div class="d-flex">
                    <div class="toast-body">
                        <strong>${title}</strong><br/>${message}
                    </div>
                    <button type="button" class="btn-close btn-close-white me-2 m-auto" data-bs-dismiss="toast" aria-label="Close"></button>
                </div>
            </div>
        `;
        
        $('#toast-container').append(toastHtml);
        const toastElement = document.getElementById(toastId);
        const toast = new bootstrap.Toast(toastElement);
        toast.show();

        // Удаляем из DOM после скрытия
        toastElement.addEventListener('hidden.bs.toast', function () {
            toastElement.remove();
        });
    };

    // 3. Загрузка текущей корзины при старте (обновление счетчика)
    updateCartCount();
});

/**
 * Обновляет счетчик товаров в шапке сайта.
 */
function updateCartCount() {
    $.get('/cart/api/count', function(data) {
        // Ожидаем JSON: { count: 5 }
        const count = data.count || data.totalItems || 0;
        $('.cart-count-badge').text(count).toggle(count > 0);
    }).fail(function() {
        // Если API еще нет или ошибка, скрываем бейдж
        $('.cart-count-badge').hide();
    });
}