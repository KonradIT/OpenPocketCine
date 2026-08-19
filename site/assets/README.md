# Public-site assets

Only deploy-ready files belong here. Raw captures, PSD sources, and personal
footage stay in gitignored `.local/`. App LUTs live under
`ios/OpenPocketCine/Resources/`, not this tree.

`screens/*.webp` are reviewed field-usage mockups for the README and landing
page. Encode with:

```bash
cwebp -q 82 -alpha_q 90 -resize 1600 0 input.png -o site/assets/screens/name.webp
```

Names must be lowercase kebab-case. Each file must stay ≤ 1 MiB.
