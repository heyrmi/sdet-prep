import test from '@lib/BaseTest';

/**
 * Native Playwright visual-regression coverage.
 *
 * These specs are tagged `@visual` and, per playwright.config.ts, only execute
 * on the Chrome (chromium) project — cross-browser projects exclude the tag via
 * `grepInvert` so the committed baseline set stays small and deterministic.
 *
 * Target pages are deliberately the lowest-dynamism static pages of
 * the-internet.herokuapp.com. Baselines live under `visual-baselines/`.
 * Regenerate with: `npm run test:visual -- --update-snapshots`.
 */
test.describe('Visual regression @visual', () => {
	test('home page renders as baseline @visual', async ({
		homePage,
		visualCheck,
	}) => {
		await homePage.goto();
		await homePage.page.waitForLoadState('networkidle');
		await visualCheck('home-page.png', {
			fullPage: true,
			maxDiffPixelRatio: 0.02,
		});
	});

	test('checkboxes page renders as baseline @visual', async ({
		checkBoxesPage,
		visualCheck,
	}) => {
		await checkBoxesPage.goto();
		await checkBoxesPage.page.waitForLoadState('networkidle');
		await visualCheck('checkboxes-page.png', {
			fullPage: true,
			maxDiffPixelRatio: 0.02,
		});
	});

	test('dropdown page renders as baseline @visual', async ({
		dropDownPage,
		visualCheck,
	}) => {
		await dropDownPage.goto();
		await dropDownPage.page.waitForLoadState('networkidle');
		await visualCheck('dropdown-page.png', {
			fullPage: true,
			maxDiffPixelRatio: 0.02,
		});
	});

	test('data tables page renders as baseline @visual', async ({
		dataTablePage,
		visualCheck,
	}) => {
		await dataTablePage.goto();
		await dataTablePage.page.waitForLoadState('networkidle');
		await visualCheck('data-tables-page.png', {
			fullPage: true,
			maxDiffPixelRatio: 0.02,
		});
	});
});
