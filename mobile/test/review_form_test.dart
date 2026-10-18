import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:futslot/core/theme.dart';
import 'package:futslot/shared/review_form.dart';

void main() {
  testWidgets('a screen reader can pick the stars and post', (tester) async {
    final handle = tester.ensureSemantics();
    (int, String?)? saved;
    await tester.pumpWidget(MaterialApp(
      theme: buildTheme(Brightness.light),
      home: Scaffold(body: ReviewForm(onSaved: (rating, comment) => saved = (rating, comment))),
    ));

    // Fails unless the star offers screen readers a tap
    tester.semantics.tap(find.semantics.byLabel('4 out of 5: Good'));
    await tester.pump();
    await tester.tap(find.text('Post review'));

    expect(saved, (4, null));
    handle.dispose();
  });
}
