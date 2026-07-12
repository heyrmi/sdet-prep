import type { APIResponse } from '@playwright/test';
import { expect } from '@playwright/test';

export class ApiResponse<T> {
	constructor(private readonly response: APIResponse) {}

	get status(): number {
		return this.response.status();
	}

	get headers(): Record<string, string> {
		return this.response.headers();
	}

	get url(): string {
		return this.response.url();
	}

	async json(): Promise<T> {
		return (await this.response.json()) as T;
	}

	async text(): Promise<string> {
		return this.response.text();
	}

	async body(): Promise<Buffer> {
		return this.response.body();
	}

	expectStatus(expected: number): this {
		expect(this.status, `Expected status ${expected}, got ${this.status}`).toBe(
			expected,
		);
		return this;
	}

	expectHeader(name: string, expected: string | RegExp): this {
		const value = this.headers[name.toLowerCase()];
		if (typeof expected === 'string') {
			expect(value).toBe(expected);
		} else {
			expect(value).toMatch(expected);
		}
		return this;
	}

	expectStatusIn(codes: number[]): this {
		expect(codes).toContain(this.status);
		return this;
	}
}
