import { FreeApiClient } from '@lib/api/FreeApiClient';
import { test as baseTest, expect } from '@playwright/test';
import { testConfig } from '@/testConfig';

// Use raw baseTest — this file manages its own auth lifecycle
const test = baseTest;

async function createClientWithContext(
	playwright: typeof import('playwright-core'),
	extraHeaders?: Record<string, string>,
) {
	const ctx = await playwright.request.newContext({
		baseURL: testConfig.freeApiUrl,
		...(extraHeaders && { extraHTTPHeaders: extraHeaders }),
	});
	return { client: new FreeApiClient(ctx), ctx };
}

test.describe('Session Management', () => {
	test.describe.configure({ mode: 'serial' });

	let accessToken: string;
	let username: string;

	test('should register and login, storing the session token', async ({
		playwright,
	}) => {
		const { client, ctx } = await createClientWithContext(playwright);

		const uniqueId = `session_${Date.now()}_${Math.random().toString(36).slice(2, 8)}`;
		username = uniqueId;

		await client.register(`${uniqueId}@test.com`, uniqueId, 'TestPass123');
		const loginRes = await client.login(uniqueId, 'TestPass123');
		const loginBody = await loginRes.json();

		expect(loginBody.success).toBe(true);
		accessToken = loginBody.data.accessToken;
		expect(accessToken).toBeTruthy();

		await ctx.dispose();
	});

	test('should use stored token to access protected route', async ({
		playwright,
	}) => {
		const { client, ctx } = await createClientWithContext(playwright, {
			Authorization: `Bearer ${accessToken}`,
		});

		const response = await client.getCurrentUser();
		response.expectStatus(200);

		const body = await response.json();
		expect(body.success).toBe(true);
		expect(body.data.username).toBe(username);

		await ctx.dispose();
	});

	test('should use stored token for multiple sequential calls', async ({
		playwright,
	}) => {
		const { client, ctx } = await createClientWithContext(playwright, {
			Authorization: `Bearer ${accessToken}`,
		});

		// First call — create a todo
		const createRes = await client.createTodo(
			'Session Test',
			'Testing token reuse',
		);
		createRes.expectStatus(201);

		// Second call — list todos (same token)
		const listRes = await client.getTodos();
		listRes.expectStatus(200);

		const listBody = await listRes.json();
		expect(listBody.success).toBe(true);

		await ctx.dispose();
	});

	test('should fail with an invalid token', async ({ playwright }) => {
		const { client, ctx } = await createClientWithContext(playwright, {
			Authorization: 'Bearer invalid.token.here',
		});

		const response = await client.getCurrentUser();
		response.expectStatus(401);

		const body = await response.json();
		expect(body.success).toBe(false);

		await ctx.dispose();
	});
});
