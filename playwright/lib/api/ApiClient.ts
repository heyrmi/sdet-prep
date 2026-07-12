import type { APIRequestContext } from '@playwright/test';
import type { RequestOptions } from '@/types/api';
import { ApiResponse } from './ApiResponse';

export { ApiResponse } from './ApiResponse';

export class ApiClient {
	constructor(protected readonly request: APIRequestContext) {}

	async get<T>(url: string, options?: RequestOptions): Promise<ApiResponse<T>> {
		const response = await this.request.get(url, options);
		return new ApiResponse<T>(response);
	}

	async post<T>(
		url: string,
		options?: RequestOptions,
	): Promise<ApiResponse<T>> {
		const response = await this.request.post(url, options);
		return new ApiResponse<T>(response);
	}

	async put<T>(url: string, options?: RequestOptions): Promise<ApiResponse<T>> {
		const response = await this.request.put(url, options);
		return new ApiResponse<T>(response);
	}

	async patch<T>(
		url: string,
		options?: RequestOptions,
	): Promise<ApiResponse<T>> {
		const response = await this.request.patch(url, options);
		return new ApiResponse<T>(response);
	}

	async delete<T>(
		url: string,
		options?: RequestOptions,
	): Promise<ApiResponse<T>> {
		const response = await this.request.delete(url, options);
		return new ApiResponse<T>(response);
	}

	async head<T>(
		url: string,
		options?: RequestOptions,
	): Promise<ApiResponse<T>> {
		const response = await this.request.head(url, options);
		return new ApiResponse<T>(response);
	}
}
