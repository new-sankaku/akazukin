// Akazukin - HTMX initialization, JWT management, and UI helpers

interface HtmxConfigRequestEvent extends Event {
    detail: {
        headers: Record<string, string>;
    };
}

interface HtmxResponseErrorEvent extends Event {
    detail: {
        xhr: XMLHttpRequest;
        target: HTMLElement;
    };
}

interface LoginResponse {
    accessToken: string;
    refreshToken: string;
    expiresIn: number;
}

interface ToastOptions {
    message: string;
    type: "success" | "error" | "info" | "warning";
    duration?: number;
}

// ---------------------------------------------------------------------------
// Token Management
// ---------------------------------------------------------------------------

const TOKEN_KEYS = {
    ACCESS: "accessToken",
    REFRESH: "refreshToken",
    EXPIRES_AT: "tokenExpiresAt",
} as const;

const REFRESH_ENDPOINT = "/api/v1/auth/refresh";
const TOKEN_REFRESH_MARGIN_MS = 60_000;

let refreshInProgress: Promise<boolean> | null = null;

function storeTokens(response: LoginResponse): void {
    localStorage.setItem(TOKEN_KEYS.ACCESS, response.accessToken);
    localStorage.setItem(TOKEN_KEYS.REFRESH, response.refreshToken);
    const expiresAt = Date.now() + response.expiresIn * 1000;
    localStorage.setItem(TOKEN_KEYS.EXPIRES_AT, expiresAt.toString());
    scheduleTokenRefresh(response.expiresIn * 1000);
}

function getAccessToken(): string | null {
    return localStorage.getItem(TOKEN_KEYS.ACCESS);
}

function getRefreshToken(): string | null {
    return localStorage.getItem(TOKEN_KEYS.REFRESH);
}

function clearTokens(): void {
    localStorage.removeItem(TOKEN_KEYS.ACCESS);
    localStorage.removeItem(TOKEN_KEYS.REFRESH);
    localStorage.removeItem(TOKEN_KEYS.EXPIRES_AT);
}

function isTokenExpiringSoon(): boolean {
    const expiresAt = localStorage.getItem(TOKEN_KEYS.EXPIRES_AT);
    if (!expiresAt) {
        return true;
    }
    return Date.now() >= parseInt(expiresAt, 10) - TOKEN_REFRESH_MARGIN_MS;
}

async function refreshAccessToken(): Promise<boolean> {
    if (refreshInProgress) {
        return refreshInProgress;
    }

    const currentRefreshToken = getRefreshToken();
    if (!currentRefreshToken) {
        return false;
    }

    refreshInProgress = (async (): Promise<boolean> => {
        try {
            const response = await fetch(REFRESH_ENDPOINT, {
                method: "POST",
                headers: { "Content-Type": "application/json" },
                body: JSON.stringify({ refreshToken: currentRefreshToken }),
            });

            if (!response.ok) {
                clearTokens();
                return false;
            }

            const data: LoginResponse = await response.json();
            storeTokens(data);
            return true;
        } catch {
            clearTokens();
            return false;
        } finally {
            refreshInProgress = null;
        }
    })();

    return refreshInProgress;
}

let refreshTimerId: ReturnType<typeof setTimeout> | null = null;

function scheduleTokenRefresh(expiresInMs: number): void {
    if (refreshTimerId !== null) {
        clearTimeout(refreshTimerId);
    }
    const delay = Math.max(expiresInMs - TOKEN_REFRESH_MARGIN_MS, 0);
    refreshTimerId = setTimeout(() => {
        refreshAccessToken().then((success) => {
            if (!success) {
                redirectToLogin();
            }
        });
    }, delay);
}

function redirectToLogin(): void {
    clearTokens();
    window.location.href = "/";
}

// ---------------------------------------------------------------------------
// HTMX Interceptors
// ---------------------------------------------------------------------------

function initHtmxInterceptors(): void {
    document.body.addEventListener("htmx:configRequest", ((event: HtmxConfigRequestEvent) => {
        const token = getAccessToken();
        if (token) {
            event.detail.headers["Authorization"] = "Bearer " + token;
        }
    }) as EventListener);

    document.body.addEventListener("htmx:responseError", ((event: HtmxResponseErrorEvent) => {
        const status = event.detail.xhr.status;
        if (status === 401) {
            refreshAccessToken().then((success) => {
                if (success) {
                    const target = event.detail.target;
                    if (target && typeof (window as Record<string, unknown>)["htmx"] !== "undefined") {
                        (window as Record<string, { trigger: (elt: HTMLElement, event: string) => void }>)
                            ["htmx"].trigger(target, "htmx:load");
                    }
                } else {
                    redirectToLogin();
                }
            });
        } else if (status >= 500) {
            showToast({
                message: "Server error occurred. Please try again later.",
                type: "error",
            });
        }
    }) as EventListener);
}

// ---------------------------------------------------------------------------
// Theme Toggle
// ---------------------------------------------------------------------------

function initTheme(): void {
    const saved = localStorage.getItem("theme");
    if (saved) {
        document.documentElement.setAttribute("data-theme", saved);
    } else if (window.matchMedia("(prefers-color-scheme: dark)").matches) {
        document.documentElement.setAttribute("data-theme", "dark");
    }
}

function toggleTheme(): void {
    const html = document.documentElement;
    const current = html.getAttribute("data-theme");
    const next = current === "dark" ? "light" : "dark";
    html.setAttribute("data-theme", next);
    localStorage.setItem("theme", next);

    const icon = document.getElementById("theme-icon");
    if (icon) {
        icon.textContent = next === "dark" ? "Light" : "Dark";
    }
}

// Expose globally for onclick handlers in templates
(window as Record<string, unknown>)["toggleTheme"] = toggleTheme;

// ---------------------------------------------------------------------------
// Toast Notification System
// ---------------------------------------------------------------------------

let toastContainer: HTMLElement | null = null;

function getToastContainer(): HTMLElement {
    if (toastContainer && document.body.contains(toastContainer)) {
        return toastContainer;
    }

    toastContainer = document.createElement("div");
    toastContainer.id = "ak-toast-container";
    toastContainer.setAttribute("role", "status");
    toastContainer.setAttribute("aria-live", "polite");
    Object.assign(toastContainer.style, {
        position: "fixed",
        top: "calc(var(--ak-header-height, 60px) + 1rem)",
        right: "1rem",
        zIndex: "9999",
        display: "flex",
        flexDirection: "column",
        gap: "0.5rem",
        maxWidth: "400px",
        width: "100%",
        pointerEvents: "none",
    });
    document.body.appendChild(toastContainer);
    return toastContainer;
}

const TOAST_COLORS: Record<ToastOptions["type"], { bg: string; border: string }> = {
    success: { bg: "#d4edda", border: "#28a745" },
    error: { bg: "#f8d7da", border: "#dc3545" },
    info: { bg: "#d1ecf1", border: "#17a2b8" },
    warning: { bg: "#fff3cd", border: "#ffc107" },
};

function showToast(options: ToastOptions): void {
    const container = getToastContainer();
    const toast = document.createElement("div");
    const colors = TOAST_COLORS[options.type];
    const duration = options.duration ?? 5000;

    Object.assign(toast.style, {
        background: colors.bg,
        border: "1px solid " + colors.border,
        borderLeft: "4px solid " + colors.border,
        borderRadius: "6px",
        padding: "0.8rem 1.2rem",
        color: "#212529",
        fontSize: "0.9rem",
        pointerEvents: "auto",
        opacity: "0",
        transform: "translateX(100%)",
        transition: "opacity 0.3s ease, transform 0.3s ease",
    });
    toast.textContent = options.message;

    container.appendChild(toast);

    requestAnimationFrame(() => {
        toast.style.opacity = "1";
        toast.style.transform = "translateX(0)";
    });

    setTimeout(() => {
        toast.style.opacity = "0";
        toast.style.transform = "translateX(100%)";
        setTimeout(() => {
            toast.remove();
        }, 300);
    }, duration);
}

(window as Record<string, unknown>)["showToast"] = showToast;

// ---------------------------------------------------------------------------
// Form Submission Helpers
// ---------------------------------------------------------------------------

interface SubmitOptions {
    url: string;
    method?: string;
    body: Record<string, unknown>;
    onSuccess?: (data: unknown) => void;
    onError?: (error: { message: string; error?: string }) => void;
    submitButton?: HTMLButtonElement | null;
}

async function submitForm(options: SubmitOptions): Promise<void> {
    const { url, method = "POST", body, onSuccess, onError, submitButton } = options;

    if (submitButton) {
        submitButton.disabled = true;
        submitButton.setAttribute("aria-busy", "true");
    }

    const token = getAccessToken();
    if (!token) {
        redirectToLogin();
        return;
    }

    try {
        let response = await fetch(url, {
            method,
            headers: {
                "Content-Type": "application/json",
                "Authorization": "Bearer " + token,
            },
            body: JSON.stringify(body),
        });

        if (response.status === 401) {
            const refreshed = await refreshAccessToken();
            if (!refreshed) {
                redirectToLogin();
                return;
            }
            const newToken = getAccessToken();
            response = await fetch(url, {
                method,
                headers: {
                    "Content-Type": "application/json",
                    "Authorization": "Bearer " + newToken,
                },
                body: JSON.stringify(body),
            });
        }

        if (!response.ok) {
            const errorData = await response.json();
            if (onError) {
                onError(errorData);
            } else {
                showToast({
                    message: errorData.message || "Request failed",
                    type: "error",
                });
            }
            return;
        }

        const contentType = response.headers.get("Content-Type");
        const data = contentType && contentType.includes("application/json")
            ? await response.json()
            : null;

        if (onSuccess) {
            onSuccess(data);
        } else {
            showToast({ message: "Success!", type: "success" });
        }
    } catch {
        if (onError) {
            onError({ message: "Network error. Please check your connection and try again." });
        } else {
            showToast({
                message: "Network error. Please check your connection and try again.",
                type: "error",
            });
        }
    } finally {
        if (submitButton) {
            submitButton.disabled = false;
            submitButton.removeAttribute("aria-busy");
        }
    }
}

(window as Record<string, unknown>)["submitForm"] = submitForm;

// ---------------------------------------------------------------------------
// Loading State Management
// ---------------------------------------------------------------------------

function showLoading(element: HTMLElement): void {
    element.setAttribute("aria-busy", "true");
    element.classList.add("ak-loading");
}

function hideLoading(element: HTMLElement): void {
    element.removeAttribute("aria-busy");
    element.classList.remove("ak-loading");
}

(window as Record<string, unknown>)["showLoading"] = showLoading;
(window as Record<string, unknown>)["hideLoading"] = hideLoading;

// ---------------------------------------------------------------------------
// Character Counter for Post Compose
// ---------------------------------------------------------------------------

interface PlatformLimit {
    name: string;
    maxLength: number;
}

const PLATFORM_LIMITS: PlatformLimit[] = [
    { name: "Twitter / X", maxLength: 280 },
    { name: "Bluesky", maxLength: 300 },
    { name: "Mastodon", maxLength: 500 },
    { name: "Threads", maxLength: 500 },
    { name: "Reddit", maxLength: 40000 },
    { name: "Telegram", maxLength: 4096 },
    { name: "VK", maxLength: 15895 },
    { name: "Pinterest", maxLength: 500 },
];

function initCharacterCounter(
    textareaId: string,
    counterElementId: string
): void {
    const textarea = document.getElementById(textareaId) as HTMLTextAreaElement | null;
    const counter = document.getElementById(counterElementId);

    if (!textarea || !counter) {
        return;
    }

    function updateCounter(): void {
        if (!textarea || !counter) {
            return;
        }
        const length = textarea.value.length;
        const warnings: string[] = [];

        for (const platform of PLATFORM_LIMITS) {
            if (length > platform.maxLength) {
                warnings.push(platform.name + " (" + platform.maxLength + ")");
            }
        }

        if (warnings.length > 0) {
            counter.textContent = length + " characters - exceeds limit for: " + warnings.join(", ");
            counter.style.color = "var(--ak-primary)";
        } else {
            counter.textContent = length + " characters";
            counter.style.color = "var(--ak-text-muted)";
        }
    }

    textarea.addEventListener("input", updateCounter);
    updateCounter();
}

(window as Record<string, unknown>)["initCharacterCounter"] = initCharacterCounter;

// ---------------------------------------------------------------------------
// Mobile Menu Toggle
// ---------------------------------------------------------------------------

function toggleMobileMenu(): void {
    const sidebar = document.querySelector(".ak-sidebar") as HTMLElement | null;
    if (sidebar) {
        sidebar.classList.toggle("ak-sidebar--open");
    }
}

(window as Record<string, unknown>)["toggleMobileMenu"] = toggleMobileMenu;

// ---------------------------------------------------------------------------
// Logout
// ---------------------------------------------------------------------------

function logout(): void {
    clearTokens();
    window.location.href = "/";
}

(window as Record<string, unknown>)["logout"] = logout;

// ---------------------------------------------------------------------------
// Initialization
// ---------------------------------------------------------------------------

initTheme();

document.addEventListener("DOMContentLoaded", () => {
    initHtmxInterceptors();

    // Schedule refresh if tokens already exist
    const expiresAtStr = localStorage.getItem(TOKEN_KEYS.EXPIRES_AT);
    if (expiresAtStr) {
        const remaining = parseInt(expiresAtStr, 10) - Date.now();
        if (remaining > 0) {
            scheduleTokenRefresh(remaining);
        } else if (getRefreshToken()) {
            refreshAccessToken().then((success) => {
                if (!success && window.location.pathname !== "/") {
                    redirectToLogin();
                }
            });
        }
    }
});
