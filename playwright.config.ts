import { defineConfig, devices } from '@playwright/test';
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

// Maturity specs (visual regression + accessibility) run ONLY on the dedicated
// bundled-Chromium project so results/baselines are small, deterministic, and
// reproducible in any environment (no system Chrome/Edge required). Every other
// project excludes the @visual/@a11y tags to avoid multiplying them across
// browsers.
const excludeMaturity = { grepInvert: /@visual|@a11y/ };

export default defineConfig({
	testDir: './tests',
	outputDir: './test-results',

	globalSetup: './globalSetup.ts',

	forbidOnly: !!process.env.CI,

	// Visual baselines live under visual-baselines/ (committed), organized by
	// project + spec so cross-browser runs never clobber each other.
	snapshotPathTemplate:
		'visual-baselines/{projectName}/{testFilePath}/{arg}{ext}',

	expect: {
		timeout: 10_000,
		toHaveScreenshot: {
			maxDiffPixelRatio: 0.02,
			animations: 'disabled',
			scale: 'css',
		},
	},

	fullyParallel: true,
	workers: process.env.CI ? 4 : undefined,
	retries: process.env.CI ? 2 : 0,

	// For large cross-browser CI runs, shard with e.g.
	// `npx playwright test --shard=1/4` across parallel machines.

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
		// Dedicated bundled-Chromium project that runs ONLY the maturity specs
		// (@visual + @a11y). Uses the version-pinned Playwright chromium so
		// committed baselines are reproducible without a system browser.
		{
			name: 'Chromium',
			testDir: './tests/ui',
			grep: /@visual|@a11y/,
			use: {
				browserName: 'chromium',
				viewport: desktopViewport,
			},
		},

		{
			name: 'Chrome',
			testDir: './tests/ui',
			use: {
				browserName: 'chromium',
				channel: 'chrome',
				viewport: desktopViewport,
			},
			...excludeMaturity,
		},

		{
			name: 'Firefox',
			testDir: './tests/ui',
			use: {
				browserName: 'firefox',
				viewport: desktopViewport,
			},
			...excludeMaturity,
		},

		{
			name: 'Edge',
			testDir: './tests/ui',
			use: {
				browserName: 'chromium',
				channel: 'msedge',
				viewport: desktopViewport,
			},
			...excludeMaturity,
		},

		{
			name: 'Safari',
			testDir: './tests/ui',
			use: {
				browserName: 'webkit',
				viewport: desktopViewport,
			},
			...excludeMaturity,
		},

		{
			name: 'Mobile Chrome',
			testDir: './tests/ui',
			use: {
				...devices['Pixel 7'],
			},
			...excludeMaturity,
		},

		{
			name: 'Mobile Safari',
			testDir: './tests/ui',
			use: {
				...devices['iPhone 15 Pro Max'],
			},
			...excludeMaturity,
		},

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
