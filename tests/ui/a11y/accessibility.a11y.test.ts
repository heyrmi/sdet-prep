import test from '@lib/BaseTest';
import { expect } from '@playwright/test';
import type { Result } from 'axe-core';

/**
 * Accessibility coverage via @axe-core/playwright.
 *
 * Each spec navigates to a page, runs an axe scan filtered to WCAG A/AA tags
 * (default of the `axeScan` fixture), attaches a readable JSON summary to the
 * report, prints a console summary, and asserts there are zero `critical`
 * violations. The total violation count is also surfaced for trend tracking.
 */

type ViolationSummary = {
	url: string;
	total: number;
	byImpact: Record<string, number>;
	violations: {
		id: string;
		impact: string | null | undefined;
		help: string;
		nodes: number;
	}[];
};

function summarize(url: string, violations: Result[]): ViolationSummary {
	const byImpact: Record<string, number> = {};
	for (const v of violations) {
		const impact = v.impact ?? 'unknown';
		byImpact[impact] = (byImpact[impact] ?? 0) + 1;
	}

	return {
		url,
		total: violations.length,
		byImpact,
		violations: violations.map((v) => ({
			id: v.id,
			impact: v.impact,
			help: v.help,
			nodes: v.nodes.length,
		})),
	};
}

async function scanAndReport(
	page: { url(): string },
	violations: Result[],
	testInfo: import('@playwright/test').TestInfo,
): Promise<ViolationSummary> {
	const summary = summarize(page.url(), violations);

	await testInfo.attach('a11y-summary.json', {
		body: JSON.stringify(summary, null, 2),
		contentType: 'application/json',
	});

	console.log(
		`[a11y] ${summary.url} -> ${summary.total} violations`,
		summary.byImpact,
	);

	return summary;
}

test.describe('Accessibility (WCAG 2 A/AA) @a11y', () => {
	test('home page has no critical a11y violations @a11y', async ({
		homePage,
		axeScan,
	}, testInfo) => {
		await homePage.goto();

		const results = await axeScan().analyze();
		const summary = await scanAndReport(
			homePage.page,
			results.violations,
			testInfo,
		);

		expect(summary.byImpact.critical ?? 0).toBe(0);
	});

	test('login page has no critical a11y violations @a11y', async ({
		loginPage,
		axeScan,
	}, testInfo) => {
		await loginPage.goto();

		// Scope to WCAG 2.0 A/AA only to demonstrate tag filtering.
		const results = await axeScan(['wcag2a', 'wcag2aa']).analyze();
		const summary = await scanAndReport(
			loginPage.page,
			results.violations,
			testInfo,
		);

		expect(summary.byImpact.critical ?? 0).toBe(0);
	});

	// Demonstrates the scanner catching a *real* WCAG defect: the dropdown page's
	// <select> ships without an accessible name (axe rule `select-name`,
	// critical). We assert the scan surfaces it, proving the gate has teeth.
	test('dropdown page: axe detects the known select-name defect @a11y', async ({
		dropDownPage,
		axeScan,
	}, testInfo) => {
		await dropDownPage.goto();

		const results = await axeScan().analyze();
		const summary = await scanAndReport(
			dropDownPage.page,
			results.violations,
			testInfo,
		);

		const selectName = summary.violations.find((v) => v.id === 'select-name');
		expect(
			selectName,
			'expected axe to flag the unlabeled <select>',
		).toBeTruthy();
		expect(selectName?.impact).toBe('critical');
	});
});
