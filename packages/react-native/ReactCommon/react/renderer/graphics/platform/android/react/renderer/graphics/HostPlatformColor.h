/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

#pragma once

#include <react/renderer/graphics/ColorComponents.h>
#include <cmath>

namespace facebook::react {

using Color = int32_t;

namespace HostPlatformColor {
static const facebook::react::Color UndefinedColor =
    std::numeric_limits<facebook::react::Color>::max();
}

inline Color hostPlatformColorFromComponents(ColorComponents components) {
  float ratio = 255;
  return ((int)round(components.alpha * ratio) & 0xff) << 24 |
      ((int)round(components.red * ratio) & 0xff) << 16 |
      ((int)round(components.green * ratio) & 0xff) << 8 |
      ((int)round(components.blue * ratio) & 0xff);
}

inline ColorComponents colorComponentsFromHostPlatformColor(Color color) {
  float ratio = 255;
  return ColorComponents{
      (float)((color >> 16) & 0xff) / ratio,
      (float)((color >> 8) & 0xff) / ratio,
      (float)((color >> 0) & 0xff) / ratio,
      (float)((color >> 24) & 0xff) / ratio};
}

} // namespace facebook::react
