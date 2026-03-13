// HTMX initialization and global configuration
document.addEventListener('DOMContentLoaded', function() {
    // Configure HTMX to include JWT token in all requests
    document.body.addEventListener('htmx:configRequest', function(event) {
        const token = localStorage.getItem('accessToken');
        if (token) {
            event.detail.headers['Authorization'] = 'Bearer ' + token;
        }
    });

    // Handle 401 responses globally - redirect to login
    document.body.addEventListener('htmx:responseError', function(event) {
        if (event.detail.xhr.status === 401) {
            localStorage.removeItem('accessToken');
            localStorage.removeItem('refreshToken');
            window.location.href = '/';
        }
    });
});
