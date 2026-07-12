import { FreeApiClient } from '@lib/api/FreeApiClient';
import { buildRegistration } from '@lib/data';
import { test as baseTest } from '@playwright/test';
import { testConfig } from '@/testConfig';

type FreeApiFixtures = {
	freeApi: FreeApiClient;
	authedFreeApi: FreeApiClient;
};

const test = baseTest.extend<FreeApiFixtures>({
	freeApi: async ({ playwright }, use): Promise<void> => {
		const context = await playwright.request.newContext({
			baseURL: testConfig.freeApiUrl,
		});
		await use(new FreeApiClient(context));
		await context.dispose();
	},

	authedFreeApi: async ({ playwright }, use): Promise<void> => {
		// Dynamic, unique credentials come from the shared data factory.
		const { email, username, password } = buildRegistration();

		// Register and login to get a bearer token
		const registerCtx = await playwright.request.newContext({
			baseURL: testConfig.freeApiUrl,
		});
		const registerClient = new FreeApiClient(registerCtx);

		await registerClient.register(email, username, password);
		const loginResponse = await registerClient.login(username, password);
		const loginBody = await loginResponse.json();
		if (!loginBody.data?.accessToken) {
			throw new Error(`FreeAPI fixture login failed: ${loginBody.message}`);
		}
		const token = loginBody.data.accessToken;
		await registerCtx.dispose();

		// Create authenticated context with bearer token
		const authedCtx = await playwright.request.newContext({
			baseURL: testConfig.freeApiUrl,
			extraHTTPHeaders: {
				Authorization: `Bearer ${token}`,
			},
		});
		await use(new FreeApiClient(authedCtx));
		await authedCtx.dispose();
	},
});

export default test;
