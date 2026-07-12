import test from '@lib/BaseTest';
import { expect } from '@playwright/test';

test.describe('Frames Page Tests', () => {
	test.describe('Main Frames Page', () => {
		test('should display correct heading', async ({ framesPage }) => {
			await framesPage.goto();
			await expect(framesPage.heading).toHaveText('Frames');
		});

		test('should have links to the nested frames and iframe', async ({
			framesPage,
		}) => {
			await framesPage.goto();
			await expect(framesPage.nestedFramesLink).toBeVisible();
			await expect(framesPage.iFrameLink).toBeVisible();
		});
	});

	test.describe('Nested frames', () => {
		test('should display correct text in each frame @smoke', async ({
			nestedFramesPage,
		}) => {
			await nestedFramesPage.goto();

			expect(await nestedFramesPage.getLeftFrameText()).toContain('LEFT');
			expect(await nestedFramesPage.getMiddleFrameText()).toContain('MIDDLE');
			expect(await nestedFramesPage.getRightFrameText()).toContain('RIGHT');
			expect(await nestedFramesPage.getBottomFrameText()).toContain('BOTTOM');
		});

		test('should access nested frame inside top frame', async ({
			nestedFramesPage,
		}) => {
			await nestedFramesPage.goto();

			const middleText = await nestedFramesPage.getMiddleFrameText();
			expect(middleText).toContain('MIDDLE');
		});
	});

	test.describe('iFrame (TinyMCE Editor)', () => {
		test('should display editor heading', async ({ iFramePage }) => {
			await iFramePage.goto();
			await expect(iFramePage.heading).toHaveText(
				'An iFrame containing the TinyMCE WYSIWYG Editor',
			);
		});

		test.fixme('should have default text in editor', async ({ iFramePage }) => {
			await iFramePage.goto();
			const text = await iFramePage.getEditorText();
			expect(text).toContain('Your content goes here.');
		});

		// Skipping below tests since this page uses paid editor for which the freemium plan is expired.
		test.fixme('should clear and type a new text', async ({ iFramePage }) => {
			await iFramePage.goto();
			await iFramePage.clearEditor();
			await iFramePage.typeInEditor('Hello Playwright.');
			expect(await iFramePage.getEditorText()).toBe('Hello Playwright.');
		});

		test.fixme(
			'should append text to existing content',
			async ({ iFramePage }) => {
				await iFramePage.goto();
				await iFramePage.clearEditor();
				await iFramePage.typeInEditor('First Line.');
				await iFramePage.typeInEditor(' Second Line.');

				expect(await iFramePage.getEditorText()).toContain(
					'First Line. Second Line.',
				);
			},
		);
	});
});
