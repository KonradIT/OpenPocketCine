# Public-site assets

Only deploy-ready files belong here. Raw captures, PSD sources, and personal
footage stay in gitignored `.local/`. App LUTs live under
`ios/OpenPocketCine/Resources/`, not this tree.

When screenshots are ready, put reviewed WebP files in `screens/` as
`kebab-case.webp` (≤ 1 MiB). Encode with:

```bash
cwebp -q 82 -alpha_q 90 -resize 1600 0 input.png -o site/assets/screens/name.webp
```

Blur faces before committing.
