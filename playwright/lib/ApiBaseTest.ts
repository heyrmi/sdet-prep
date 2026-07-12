import { HttpBinClient } from '@lib/api/HttpBinClient';
import { test as baseTest } from '@playwright/test';
import { testConfig } from '@/testConfig';

type ApiFixtures = {
	httpBin: HttpBinClient;
	authedHttpBin: HttpBinClient;
};

const test = baseTest.extend<ApiFixtures>({
	httpBin: async ({ playwright }, use): Promise<void> => {
		const context = await playwright.request.newContext({
			baseURL: testConfig.apiUrl,
		});
		await use(new HttpBinClient(context));
		await context.dispose();
	},

	authedHttpBin: async ({ playwright }, use): Promise<void> => {
		const context = await playwright.request.newContext({
			baseURL: testConfig.apiUrl,
			httpCredentials: {
				username: testConfig.username,
				password: testConfig.password,
			},
		});
		await use(new HttpBinClient(context));
		await context.dispose();
	},
});

export default test;
