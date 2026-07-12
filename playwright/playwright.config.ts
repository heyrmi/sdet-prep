import { defineConfig } from '@playwright/test';
import { testConfig } from './testConfig';

const ENV = testConfig.env;

if (!ENV || !['stage', 'prod'].includes(ENV)) {
	console.error(
		`Invalid ENV value: ${ENV}. Allowed values are 'stage' or 'prod'`,
	);
	process.exit(1);
}

// Common config
const commonUseOptions = {
	baseURL: testConfig.uiUrl,
	headless: !!process.env.CI,
	ignoreHTTPSErrors: true,
	video: 'retain-on-failure' as const,
	trace: 'retain-on-failure' as const,
	screenshot: 'only-on-failure' as const,
};

const desktopViewport = { width: 1920, height: 1080 };

export default defineConfig({
	testDir: './tests',
	outputDir: './test-results',

	globalSetup: './globalSetup.ts',

	forbidOnly: !!process.env.CI,

	expect: { timeout: 10_000 },

	fullyParallel: true,
	workers: process.env.CI ? 4 : undefined,
	retries: process.env.CI ? 2 : 0,

	reporter: [
		['list'],
		[
			'html',
			{
				title: `E2E-HTML-${ENV}`,
				open: 'on-failure',
				outputFolder: 'html-reports',
			},
		],
	],

	use: {
		...commonUseOptions,
	},

	projects: [
		{
			name: 'Chrome',
			testDir: './tests/ui',
			use: {
				browserName: 'chromium',
				channel: 'chrome',
				viewport: desktopViewport,
			},
		},

		// {
		// 	name: 'Firefox',
		//  testDir: './tests/ui',
		// 	use: {
		// 		browserName: 'firefox',
		// 		viewport: desktopViewport,
		// 	},
		// },

		// {
		// 	name: 'Edge',
		//  testDir: './tests/ui',
		// 	use: {
		// 		browserName: 'chromium',
		// 		channel: 'msedge',
		// 		viewport: desktopViewport,
		// 	},
		// },

		// {
		// 	name: 'Safari',
		//  testDir: './tests/ui',
		// 	use: {
		// 		browserName: 'webkit',
		// 		viewport: desktopViewport,
		// 	},
		// },

		// {
		// 	name: 'Mobile Chrome',
		//  testDir: './tests/ui',
		// 	use: {
		// 		...devices['Pixel 7'],
		// 	},
		// },

		// {
		// 	name: 'Mobile Safari',
		//  testDir: './tests/ui',
		// 	use: {
		// 		...devices['iPhone 15 Pro Max'],
		// 	},
		// },

		{
			name: 'API',
			testDir: './tests/api',
			use: {
				baseURL: testConfig.apiUrl,
			},
		},

		{
			name: 'FreeAPI',
			testDir: './tests/freeapi',
			use: {
				baseURL: testConfig.freeApiUrl,
			},
		},
	],
});
