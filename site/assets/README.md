# Public-site assets

Only deploy-ready files belong here. Raw captures, PSD sources, and personal
footage stay in gitignored `.local/`. App LUTs live under
`ios/OpenPocketCine/Resources/`, not this tree.

`screens/*.webp` are reviewed field-usage mockups for the README and landing
page. Flatten any transparency onto black first so GitHub's light page does
not show through, then encode as opaque WebP:

```bash
cwebp -q 82 -blend_alpha 000000 -resize 1600 0 input.png -o site/assets/screens/name.webp
```

For a portrait source, resize the long edge to 1600 (`-resize 0 1600`).
Names must be lowercase kebab-case. Each file must stay ≤ 1 MiB.
