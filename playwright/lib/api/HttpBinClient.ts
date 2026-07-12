import type { HttpBinPostResponse } from '@/types/httpbin';
import { ApiClient } from './ApiClient';

export class HttpBinClient extends ApiClient {
	async getIp() {
		return this.get<{ origin: string }>('/ip');
	}

	async getUserAgent() {
		return this.get<{ 'user-agent': string }>('/user-agent');
	}

	async getHeaders() {
		return this.get<{ headers: Record<string, string> }>('/headers');
	}

	async postJson(data: Record<string, unknown>) {
		return this.post<HttpBinPostResponse>('/post', { data });
	}

	async postForm(form: Record<string, string>) {
		return this.post<HttpBinPostResponse>('/post', { form });
	}

	async getStatus(code: number) {
		return this.get<void>(`/status/${code}`, { failOnStatusCode: false });
	}

	async getWithBasicAuth(user: string, passwd: string) {
		return this.get<{ authenticated: boolean; user: string }>(
			`/basic-auth/${user}/${passwd}`,
		);
	}

	async getUuid() {
		return this.get<{ uuid: string }>('/uuid');
	}
}
