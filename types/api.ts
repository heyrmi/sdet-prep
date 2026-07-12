export interface RequestOptions {
	data?: unknown;
	form?: Record<string, string>;
	headers?: Record<string, string>;
	params?: Record<string, string | number>;
	timeout?: number;
	failOnStatusCode?: boolean;
	maxRedirects?: number;
}
