// @ts-check
import { defineConfig } from 'astro/config';
import starlight from '@astrojs/starlight';

// https://astro.build/config
export default defineConfig({
	integrations: [
		starlight({
			title: 'TuxGuitar UAT',
			sidebar: [
				{ label: 'Writing a UAT page', slug: 'writing/page-format' },
				{
					label: 'Features',
					items: [{ autogenerate: { directory: 'features' } }],
				},
			],
		}),
	],
});
