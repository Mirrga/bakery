const registerForm = document.getElementById('registerForm');

if (registerForm) {

    registerForm.addEventListener('submit', function(event) {

        event.preventDefault();

        const firstName = document.getElementById('firstName').value;
        const lastName = document.getElementById('lastName').value;
        const email = document.getElementById('registerEmail').value;
        const password = document.getElementById('registerPassword').value;

        const user = {
            firstName: firstName,
            lastName: lastName,
            email: email,
            password: password
        };

        localStorage.setItem('user', JSON.stringify(user));

        alert('Регистрация успешна!');

        window.location.href = 'login.html';
    });
}

// LOGIN

const loginForm = document.getElementById('loginForm');

if (loginForm) {

    loginForm.addEventListener('submit', function(event) {

        event.preventDefault();

        const email = document.getElementById('loginEmail').value;
        const password = document.getElementById('loginPassword').value;

        const savedUser = JSON.parse(localStorage.getItem('user'));

        if (
            savedUser &&
            email === savedUser.email &&
            password === savedUser.password
        ) {
            alert('Вход выполнен успешно!');
            window.location.href = 'index.html';
        }
        else {
            alert('Неверный e-mail или пароль');
        }
    });
}

// RESET PASSWORD

const resetForm = document.getElementById('resetForm');

if (resetForm) {

    resetForm.addEventListener('submit', function(event) {

        event.preventDefault();

        const newPassword = document.getElementById('newPassword').value;

        const savedUser = JSON.parse(localStorage.getItem('user'));

        if (savedUser) {

            savedUser.password = newPassword;

            localStorage.setItem('user', JSON.stringify(savedUser));

            window.location.href = 'success.html';
        }
    });
}
