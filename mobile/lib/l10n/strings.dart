import 'dart:async';

import 'package:flutter/foundation.dart';
import 'package:flutter/widgets.dart';
import 'package:flutter_localizations/flutter_localizations.dart';
import 'package:intl/intl.dart' as intl;

import 'strings_en.dart';
import 'strings_ne.dart';

// ignore_for_file: type=lint

/// Callers can lookup localized strings with an instance of S
/// returned by `S.of(context)`.
///
/// Applications need to include `S.delegate()` in their app's
/// `localizationDelegates` list, and the locales they support in the app's
/// `supportedLocales` list. For example:
///
/// ```dart
/// import 'l10n/strings.dart';
///
/// return MaterialApp(
///   localizationsDelegates: S.localizationsDelegates,
///   supportedLocales: S.supportedLocales,
///   home: MyApplicationHome(),
/// );
/// ```
///
/// ## Update pubspec.yaml
///
/// Please make sure to update your pubspec.yaml to include the following
/// packages:
///
/// ```yaml
/// dependencies:
///   # Internationalization support.
///   flutter_localizations:
///     sdk: flutter
///   intl: any # Use the pinned version from flutter_localizations
///
///   # Rest of dependencies
/// ```
///
/// ## iOS Applications
///
/// iOS applications define key application metadata, including supported
/// locales, in an Info.plist file that is built into the application bundle.
/// To configure the locales supported by your app, you’ll need to edit this
/// file.
///
/// First, open your project’s ios/Runner.xcworkspace Xcode workspace file.
/// Then, in the Project Navigator, open the Info.plist file under the Runner
/// project’s Runner folder.
///
/// Next, select the Information Property List item, select Add Item from the
/// Editor menu, then select Localizations from the pop-up menu.
///
/// Select and expand the newly-created Localizations item then, for each
/// locale your application supports, add a new item and select the locale
/// you wish to add from the pop-up menu in the Value field. This list should
/// be consistent with the languages listed in the S.supportedLocales
/// property.
abstract class S {
  S(String locale)
    : localeName = intl.Intl.canonicalizedLocale(locale.toString());

  final String localeName;

  static S of(BuildContext context) {
    return Localizations.of<S>(context, S)!;
  }

  static const LocalizationsDelegate<S> delegate = _SDelegate();

  /// A list of this localizations delegate along with the default localizations
  /// delegates.
  ///
  /// Returns a list of localizations delegates containing this delegate along with
  /// GlobalMaterialLocalizations.delegate, GlobalCupertinoLocalizations.delegate,
  /// and GlobalWidgetsLocalizations.delegate.
  ///
  /// Additional delegates can be added by appending to this list in
  /// MaterialApp. This list does not have to be used at all if a custom list
  /// of delegates is preferred or required.
  static const List<LocalizationsDelegate<dynamic>> localizationsDelegates =
      <LocalizationsDelegate<dynamic>>[
        delegate,
        GlobalMaterialLocalizations.delegate,
        GlobalCupertinoLocalizations.delegate,
        GlobalWidgetsLocalizations.delegate,
      ];

  /// A list of this localizations delegate's supported locales.
  static const List<Locale> supportedLocales = <Locale>[
    Locale('en'),
    Locale('ne'),
  ];

  /// No description provided for @justNow.
  ///
  /// In en, this message translates to:
  /// **'just now'**
  String get justNow;

  /// No description provided for @minutesAgo.
  ///
  /// In en, this message translates to:
  /// **'{minutes} min ago'**
  String minutesAgo(String minutes);

  /// No description provided for @hoursAgo.
  ///
  /// In en, this message translates to:
  /// **'{hours} h ago'**
  String hoursAgo(String hours);

  /// No description provided for @methodCash.
  ///
  /// In en, this message translates to:
  /// **'Cash'**
  String get methodCash;

  /// No description provided for @methodBankTransfer.
  ///
  /// In en, this message translates to:
  /// **'Bank transfer'**
  String get methodBankTransfer;

  /// No description provided for @players.
  ///
  /// In en, this message translates to:
  /// **'{count, plural, =1{1 player} other{{count} players}}'**
  String players(int count);

  /// No description provided for @games.
  ///
  /// In en, this message translates to:
  /// **'{count, plural, =1{1 game} other{{count} games}}'**
  String games(int count);

  /// No description provided for @hours.
  ///
  /// In en, this message translates to:
  /// **'{count, plural, =1{1 hour} other{{count} hours}}'**
  String hours(int count);

  /// No description provided for @days.
  ///
  /// In en, this message translates to:
  /// **'{count, plural, =1{1 day} other{{count} days}}'**
  String days(int count);

  /// The last two items of a list: "10% within 12 hours and 50% within 2 hours"
  ///
  /// In en, this message translates to:
  /// **'{first} and {last}'**
  String listAnd(String first, String last);

  /// No description provided for @skillBeginner.
  ///
  /// In en, this message translates to:
  /// **'Beginner'**
  String get skillBeginner;

  /// No description provided for @skillIntermediate.
  ///
  /// In en, this message translates to:
  /// **'Intermediate'**
  String get skillIntermediate;

  /// No description provided for @skillAdvanced.
  ///
  /// In en, this message translates to:
  /// **'Advanced'**
  String get skillAdvanced;

  /// No description provided for @skillPro.
  ///
  /// In en, this message translates to:
  /// **'Pro'**
  String get skillPro;

  /// No description provided for @positionGoalkeeper.
  ///
  /// In en, this message translates to:
  /// **'Goalkeeper'**
  String get positionGoalkeeper;

  /// No description provided for @positionDefender.
  ///
  /// In en, this message translates to:
  /// **'Defender'**
  String get positionDefender;

  /// No description provided for @positionMidfielder.
  ///
  /// In en, this message translates to:
  /// **'Midfielder'**
  String get positionMidfielder;

  /// No description provided for @positionForward.
  ///
  /// In en, this message translates to:
  /// **'Forward'**
  String get positionForward;

  /// No description provided for @positionAny.
  ///
  /// In en, this message translates to:
  /// **'Anywhere'**
  String get positionAny;

  /// No description provided for @footLeft.
  ///
  /// In en, this message translates to:
  /// **'Left'**
  String get footLeft;

  /// No description provided for @footRight.
  ///
  /// In en, this message translates to:
  /// **'Right'**
  String get footRight;

  /// No description provided for @footBoth.
  ///
  /// In en, this message translates to:
  /// **'Both'**
  String get footBoth;

  /// No description provided for @roleCaptain.
  ///
  /// In en, this message translates to:
  /// **'Captain'**
  String get roleCaptain;

  /// No description provided for @roleViceCaptain.
  ///
  /// In en, this message translates to:
  /// **'Vice-captain'**
  String get roleViceCaptain;

  /// No description provided for @roleMember.
  ///
  /// In en, this message translates to:
  /// **'Player'**
  String get roleMember;

  /// No description provided for @formatKnockout.
  ///
  /// In en, this message translates to:
  /// **'Knockout'**
  String get formatKnockout;

  /// No description provided for @formatLeague.
  ///
  /// In en, this message translates to:
  /// **'League'**
  String get formatLeague;

  /// No description provided for @formatGroupKnockout.
  ///
  /// In en, this message translates to:
  /// **'Groups, then knockout'**
  String get formatGroupKnockout;

  /// No description provided for @tournamentDraft.
  ///
  /// In en, this message translates to:
  /// **'Draft'**
  String get tournamentDraft;

  /// No description provided for @tournamentOpen.
  ///
  /// In en, this message translates to:
  /// **'Taking teams'**
  String get tournamentOpen;

  /// No description provided for @tournamentClosed.
  ///
  /// In en, this message translates to:
  /// **'Entries closed'**
  String get tournamentClosed;

  /// No description provided for @tournamentInProgress.
  ///
  /// In en, this message translates to:
  /// **'Under way'**
  String get tournamentInProgress;

  /// No description provided for @tournamentCompleted.
  ///
  /// In en, this message translates to:
  /// **'Finished'**
  String get tournamentCompleted;

  /// No description provided for @tournamentCancelled.
  ///
  /// In en, this message translates to:
  /// **'Cancelled'**
  String get tournamentCancelled;

  /// No description provided for @stageGroup.
  ///
  /// In en, this message translates to:
  /// **'Group stage'**
  String get stageGroup;

  /// No description provided for @stageLeague.
  ///
  /// In en, this message translates to:
  /// **'League'**
  String get stageLeague;

  /// No description provided for @stageRoundOf32.
  ///
  /// In en, this message translates to:
  /// **'Round of 32'**
  String get stageRoundOf32;

  /// No description provided for @stageRoundOf16.
  ///
  /// In en, this message translates to:
  /// **'Round of 16'**
  String get stageRoundOf16;

  /// No description provided for @stageQuarterFinal.
  ///
  /// In en, this message translates to:
  /// **'Quarter-finals'**
  String get stageQuarterFinal;

  /// No description provided for @stageSemiFinal.
  ///
  /// In en, this message translates to:
  /// **'Semi-finals'**
  String get stageSemiFinal;

  /// No description provided for @stageThirdPlace.
  ///
  /// In en, this message translates to:
  /// **'Third place'**
  String get stageThirdPlace;

  /// No description provided for @stageFinal.
  ///
  /// In en, this message translates to:
  /// **'Final'**
  String get stageFinal;

  /// No description provided for @entryPendingPayment.
  ///
  /// In en, this message translates to:
  /// **'Pay the entry fee'**
  String get entryPendingPayment;

  /// No description provided for @entryConfirmed.
  ///
  /// In en, this message translates to:
  /// **'Confirmed'**
  String get entryConfirmed;

  /// No description provided for @entryWaitlisted.
  ///
  /// In en, this message translates to:
  /// **'On the waitlist'**
  String get entryWaitlisted;

  /// No description provided for @entryWithdrawn.
  ///
  /// In en, this message translates to:
  /// **'Withdrawn'**
  String get entryWithdrawn;

  /// No description provided for @entryRejected.
  ///
  /// In en, this message translates to:
  /// **'Not accepted'**
  String get entryRejected;

  /// No description provided for @ruleEqual.
  ///
  /// In en, this message translates to:
  /// **'Split equally'**
  String get ruleEqual;

  /// No description provided for @ruleLosersPayAll.
  ///
  /// In en, this message translates to:
  /// **'Losers pay all'**
  String get ruleLosersPayAll;

  /// No description provided for @ruleLosersPayPercent.
  ///
  /// In en, this message translates to:
  /// **'Losers pay more'**
  String get ruleLosersPayPercent;

  /// No description provided for @ruleLosersPayCourt.
  ///
  /// In en, this message translates to:
  /// **'Losers pay the court'**
  String get ruleLosersPayCourt;

  /// No description provided for @ruleEqualDetail.
  ///
  /// In en, this message translates to:
  /// **'Everyone pays the same, whoever wins.'**
  String get ruleEqualDetail;

  /// No description provided for @ruleLosersPayAllDetail.
  ///
  /// In en, this message translates to:
  /// **'Baazi: the losing side pays everything.'**
  String get ruleLosersPayAllDetail;

  /// No description provided for @ruleLosersPayPercentDetail.
  ///
  /// In en, this message translates to:
  /// **'Baazi: the losing side pays a share you choose, the winners the rest.'**
  String get ruleLosersPayPercentDetail;

  /// No description provided for @ruleLosersPayCourtDetail.
  ///
  /// In en, this message translates to:
  /// **'Baazi: losers pay for the court, winners for the extras.'**
  String get ruleLosersPayCourtDetail;

  /// No description provided for @resultDraw.
  ///
  /// In en, this message translates to:
  /// **'Draw'**
  String get resultDraw;

  /// No description provided for @sideWon.
  ///
  /// In en, this message translates to:
  /// **'{side} won'**
  String sideWon(String side);

  /// No description provided for @planDiscount.
  ///
  /// In en, this message translates to:
  /// **'{percent}% off every game'**
  String planDiscount(String percent);

  /// No description provided for @planFor.
  ///
  /// In en, this message translates to:
  /// **'{terms}, for {days}'**
  String planFor(String terms, String days);

  /// No description provided for @sunday.
  ///
  /// In en, this message translates to:
  /// **'Sunday'**
  String get sunday;

  /// No description provided for @monday.
  ///
  /// In en, this message translates to:
  /// **'Monday'**
  String get monday;

  /// No description provided for @tuesday.
  ///
  /// In en, this message translates to:
  /// **'Tuesday'**
  String get tuesday;

  /// No description provided for @wednesday.
  ///
  /// In en, this message translates to:
  /// **'Wednesday'**
  String get wednesday;

  /// No description provided for @thursday.
  ///
  /// In en, this message translates to:
  /// **'Thursday'**
  String get thursday;

  /// No description provided for @friday.
  ///
  /// In en, this message translates to:
  /// **'Friday'**
  String get friday;

  /// No description provided for @saturday.
  ///
  /// In en, this message translates to:
  /// **'Saturday'**
  String get saturday;

  /// No description provided for @closed.
  ///
  /// In en, this message translates to:
  /// **'Closed'**
  String get closed;

  /// No description provided for @policyPayAtVenue.
  ///
  /// In en, this message translates to:
  /// **'Book now and pay at the venue.'**
  String get policyPayAtVenue;

  /// No description provided for @policyFreeCancelAnyTime.
  ///
  /// In en, this message translates to:
  /// **'Cancel any time before the game for free.'**
  String get policyFreeCancelAnyTime;

  /// No description provided for @policyFeeDeposit.
  ///
  /// In en, this message translates to:
  /// **'What you paid covers it first; your FutSlot deposit covers the rest.'**
  String get policyFeeDeposit;

  /// No description provided for @policyFeeCapped.
  ///
  /// In en, this message translates to:
  /// **'It\'s never more than you\'ve paid.'**
  String get policyFeeCapped;

  /// No description provided for @policyVenueCancels.
  ///
  /// In en, this message translates to:
  /// **'If the venue cancels, you get everything back.'**
  String get policyVenueCancels;

  /// No description provided for @chipConfirmed.
  ///
  /// In en, this message translates to:
  /// **'Confirmed'**
  String get chipConfirmed;

  /// No description provided for @chipHeld.
  ///
  /// In en, this message translates to:
  /// **'Waiting for payment'**
  String get chipHeld;

  /// No description provided for @chipVerify.
  ///
  /// In en, this message translates to:
  /// **'Payment being checked'**
  String get chipVerify;

  /// No description provided for @chipCompleted.
  ///
  /// In en, this message translates to:
  /// **'Played'**
  String get chipCompleted;

  /// No description provided for @chipNoShow.
  ///
  /// In en, this message translates to:
  /// **'No-show'**
  String get chipNoShow;

  /// No description provided for @chipCancelled.
  ///
  /// In en, this message translates to:
  /// **'Cancelled'**
  String get chipCancelled;

  /// No description provided for @chipExpired.
  ///
  /// In en, this message translates to:
  /// **'Not paid in time'**
  String get chipExpired;

  /// No description provided for @policyAdvance.
  ///
  /// In en, this message translates to:
  /// **'Pay {percent}% up front to confirm. We hold the slot for {minutes} minutes while you pay.'**
  String policyAdvance(String percent, int minutes);

  /// No description provided for @feeWithin.
  ///
  /// In en, this message translates to:
  /// **'{percent}% within {hours}'**
  String feeWithin(String percent, String hours);

  /// No description provided for @policyFreeBefore.
  ///
  /// In en, this message translates to:
  /// **'Cancel {hours} or more before the game for free.'**
  String policyFreeBefore(String hours);

  /// No description provided for @policyLateFee.
  ///
  /// In en, this message translates to:
  /// **'Cancelling later costs a share of the booking price: {fees}. {cover}'**
  String policyLateFee(String fees, String cover);

  /// No description provided for @policyDeposit.
  ///
  /// In en, this message translates to:
  /// **'You need a {amount} FutSlot booking deposit to book here.'**
  String policyDeposit(String amount);

  /// No description provided for @policyNoShow.
  ///
  /// In en, this message translates to:
  /// **'Not showing up costs {percent}% of the booking price.'**
  String policyNoShow(String percent);

  /// No description provided for @policyStrikes.
  ///
  /// In en, this message translates to:
  /// **'{count, plural, =1{After a late cancellation or no-show here in {days} days, you pay {percent}% up front.} other{After {count} late cancellations or no-shows here in {days} days, you pay {percent}% up front.}}'**
  String policyStrikes(int count, String days, String percent);

  /// No description provided for @sendForChecking.
  ///
  /// In en, this message translates to:
  /// **'Send for checking'**
  String get sendForChecking;

  /// No description provided for @enterTransactionId.
  ///
  /// In en, this message translates to:
  /// **'Enter the transaction ID from your receipt.'**
  String get enterTransactionId;

  /// No description provided for @paidWith.
  ///
  /// In en, this message translates to:
  /// **'Paid with'**
  String get paidWith;

  /// No description provided for @amountYouSent.
  ///
  /// In en, this message translates to:
  /// **'Amount you sent'**
  String get amountYouSent;

  /// No description provided for @transactionId.
  ///
  /// In en, this message translates to:
  /// **'Transaction ID'**
  String get transactionId;

  /// No description provided for @fromYourReceipt.
  ///
  /// In en, this message translates to:
  /// **'From your payment receipt'**
  String get fromYourReceipt;

  /// No description provided for @sending.
  ///
  /// In en, this message translates to:
  /// **'Sending…'**
  String get sending;

  /// No description provided for @loading.
  ///
  /// In en, this message translates to:
  /// **'Loading'**
  String get loading;

  /// No description provided for @tryAgain.
  ///
  /// In en, this message translates to:
  /// **'Try again'**
  String get tryAgain;

  /// No description provided for @optional.
  ///
  /// In en, this message translates to:
  /// **' (optional)'**
  String get optional;

  /// No description provided for @ratingPoor.
  ///
  /// In en, this message translates to:
  /// **'Poor'**
  String get ratingPoor;

  /// No description provided for @ratingNotGreat.
  ///
  /// In en, this message translates to:
  /// **'Not great'**
  String get ratingNotGreat;

  /// No description provided for @ratingOkay.
  ///
  /// In en, this message translates to:
  /// **'Okay'**
  String get ratingOkay;

  /// No description provided for @ratingGood.
  ///
  /// In en, this message translates to:
  /// **'Good'**
  String get ratingGood;

  /// No description provided for @ratingGreat.
  ///
  /// In en, this message translates to:
  /// **'Great'**
  String get ratingGreat;

  /// No description provided for @postReview.
  ///
  /// In en, this message translates to:
  /// **'Post review'**
  String get postReview;

  /// No description provided for @pickStars.
  ///
  /// In en, this message translates to:
  /// **'Pick how many stars.'**
  String get pickStars;

  /// No description provided for @whatWasItLike.
  ///
  /// In en, this message translates to:
  /// **'What was it like?'**
  String get whatWasItLike;

  /// No description provided for @reviewHint.
  ///
  /// In en, this message translates to:
  /// **'The turf, the lights, the staff…'**
  String get reviewHint;

  /// No description provided for @saving.
  ///
  /// In en, this message translates to:
  /// **'Saving…'**
  String get saving;

  /// No description provided for @notifications.
  ///
  /// In en, this message translates to:
  /// **'Notifications'**
  String get notifications;

  /// No description provided for @offline.
  ///
  /// In en, this message translates to:
  /// **'Can\'t reach FutSlot. Check your connection and try again.'**
  String get offline;

  /// No description provided for @somethingWentWrong.
  ///
  /// In en, this message translates to:
  /// **'Something went wrong. Please try again.'**
  String get somethingWentWrong;

  /// No description provided for @tabExplore.
  ///
  /// In en, this message translates to:
  /// **'Explore'**
  String get tabExplore;

  /// No description provided for @tabBookings.
  ///
  /// In en, this message translates to:
  /// **'Bookings'**
  String get tabBookings;

  /// No description provided for @tabPlay.
  ///
  /// In en, this message translates to:
  /// **'Play'**
  String get tabPlay;

  /// No description provided for @tabMessages.
  ///
  /// In en, this message translates to:
  /// **'Messages'**
  String get tabMessages;

  /// No description provided for @tabMe.
  ///
  /// In en, this message translates to:
  /// **'Me'**
  String get tabMe;

  /// No description provided for @playGames.
  ///
  /// In en, this message translates to:
  /// **'Games'**
  String get playGames;

  /// No description provided for @playTournaments.
  ///
  /// In en, this message translates to:
  /// **'Tournaments'**
  String get playTournaments;

  /// No description provided for @playTeams.
  ///
  /// In en, this message translates to:
  /// **'Teams'**
  String get playTeams;

  /// No description provided for @fromPrice.
  ///
  /// In en, this message translates to:
  /// **'From '**
  String get fromPrice;

  /// No description provided for @enterAmountBetween.
  ///
  /// In en, this message translates to:
  /// **'Enter between {min} and {max}.'**
  String enterAmountBetween(String min, String max);

  /// No description provided for @sendAtLeastTo.
  ///
  /// In en, this message translates to:
  /// **'Send at least {amount} to'**
  String sendAtLeastTo(String amount);

  /// No description provided for @sendAmountTo.
  ///
  /// In en, this message translates to:
  /// **'Send {amount} to'**
  String sendAmountTo(String amount);

  /// No description provided for @starsOutOfFive.
  ///
  /// In en, this message translates to:
  /// **'{stars} out of 5'**
  String starsOutOfFive(String stars);

  /// No description provided for @starsOutOfFiveWord.
  ///
  /// In en, this message translates to:
  /// **'{stars} out of 5: {word}'**
  String starsOutOfFiveWord(String stars, String word);

  /// No description provided for @notificationsUnread.
  ///
  /// In en, this message translates to:
  /// **'Notifications, {count} unread'**
  String notificationsUnread(int count);

  /// No description provided for @messagesUnread.
  ///
  /// In en, this message translates to:
  /// **'Messages, {count} unread'**
  String messagesUnread(int count);

  /// No description provided for @courts.
  ///
  /// In en, this message translates to:
  /// **'{count, plural, =1{1 court} other{{count} courts}}'**
  String courts(int count);

  /// No description provided for @ratingFrom.
  ///
  /// In en, this message translates to:
  /// **'{rating} out of 5 from {reviews}'**
  String ratingFrom(String rating, String reviews);

  /// No description provided for @reviews.
  ///
  /// In en, this message translates to:
  /// **'{count, plural, =1{1 review} other{{count} reviews}}'**
  String reviews(int count);

  /// No description provided for @footSide.
  ///
  /// In en, this message translates to:
  /// **'{side} foot'**
  String footSide(String side);

  /// No description provided for @enterMobile.
  ///
  /// In en, this message translates to:
  /// **'Enter your 10-digit mobile number, like 98XXXXXXXX.'**
  String get enterMobile;

  /// No description provided for @enterPassword.
  ///
  /// In en, this message translates to:
  /// **'Enter your password.'**
  String get enterPassword;

  /// No description provided for @passwordNotSet.
  ///
  /// In en, this message translates to:
  /// **'Your account doesn\'t have a password yet. We\'ll text you a code to set one.'**
  String get passwordNotSet;

  /// No description provided for @codeSentMoment.
  ///
  /// In en, this message translates to:
  /// **'We sent you a code a moment ago. Enter it below.'**
  String get codeSentMoment;

  /// No description provided for @codeShown.
  ///
  /// In en, this message translates to:
  /// **'Texts cost money and the developer is broke, so here\'s your code instead: {code}. Real SMS comes once a few futsals sign up.'**
  String codeShown(String code);

  /// No description provided for @accountExists.
  ///
  /// In en, this message translates to:
  /// **'This number already has an account. Sign in, or reset your password if you forgot it.'**
  String get accountExists;

  /// No description provided for @noAccountYet.
  ///
  /// In en, this message translates to:
  /// **'No account uses this number yet. Create one below.'**
  String get noAccountYet;

  /// No description provided for @enterCode.
  ///
  /// In en, this message translates to:
  /// **'Enter the 6-digit code we sent.'**
  String get enterCode;

  /// No description provided for @enterName.
  ///
  /// In en, this message translates to:
  /// **'Enter your name.'**
  String get enterName;

  /// No description provided for @googleFailed.
  ///
  /// In en, this message translates to:
  /// **'Google sign-in didn\'t work. Use your number and password instead.'**
  String get googleFailed;

  /// No description provided for @signInTitle.
  ///
  /// In en, this message translates to:
  /// **'Sign in to FutSlot'**
  String get signInTitle;

  /// No description provided for @signInSubtitle.
  ///
  /// In en, this message translates to:
  /// **'Use your mobile number and password.'**
  String get signInSubtitle;

  /// No description provided for @signUpTitle.
  ///
  /// In en, this message translates to:
  /// **'Create your account'**
  String get signUpTitle;

  /// No description provided for @signUpSubtitle.
  ///
  /// In en, this message translates to:
  /// **'We\'ll text a code to check the number is yours.'**
  String get signUpSubtitle;

  /// No description provided for @resetTitle.
  ///
  /// In en, this message translates to:
  /// **'Reset your password'**
  String get resetTitle;

  /// No description provided for @resetSubtitle.
  ///
  /// In en, this message translates to:
  /// **'We\'ll text a code to your number, then you choose a new password.'**
  String get resetSubtitle;

  /// No description provided for @googleTitle.
  ///
  /// In en, this message translates to:
  /// **'One more step'**
  String get googleTitle;

  /// No description provided for @googleSubtitle.
  ///
  /// In en, this message translates to:
  /// **'Add your mobile number. Venues and teammates find you by it, so we\'ll text a code to check it\'s yours.'**
  String get googleSubtitle;

  /// No description provided for @backToVenues.
  ///
  /// In en, this message translates to:
  /// **'Back to venues'**
  String get backToVenues;

  /// No description provided for @signedOutAgain.
  ///
  /// In en, this message translates to:
  /// **'You were signed out. Please sign in again.'**
  String get signedOutAgain;

  /// No description provided for @backToSignIn.
  ///
  /// In en, this message translates to:
  /// **'Back to sign in'**
  String get backToSignIn;

  /// No description provided for @hidePassword.
  ///
  /// In en, this message translates to:
  /// **'Hide password'**
  String get hidePassword;

  /// No description provided for @showPassword.
  ///
  /// In en, this message translates to:
  /// **'Show password'**
  String get showPassword;

  /// No description provided for @mobileNumber.
  ///
  /// In en, this message translates to:
  /// **'Mobile number'**
  String get mobileNumber;

  /// No description provided for @password.
  ///
  /// In en, this message translates to:
  /// **'Password'**
  String get password;

  /// No description provided for @newPassword.
  ///
  /// In en, this message translates to:
  /// **'New password'**
  String get newPassword;

  /// No description provided for @forgotPassword.
  ///
  /// In en, this message translates to:
  /// **'Forgot password?'**
  String get forgotPassword;

  /// No description provided for @signIn.
  ///
  /// In en, this message translates to:
  /// **'Sign in'**
  String get signIn;

  /// No description provided for @signingIn.
  ///
  /// In en, this message translates to:
  /// **'Signing in…'**
  String get signingIn;

  /// No description provided for @or.
  ///
  /// In en, this message translates to:
  /// **'or'**
  String get or;

  /// No description provided for @continueWithGoogle.
  ///
  /// In en, this message translates to:
  /// **'Continue with Google'**
  String get continueWithGoogle;

  /// No description provided for @newToFutSlot.
  ///
  /// In en, this message translates to:
  /// **'New to FutSlot?'**
  String get newToFutSlot;

  /// No description provided for @createAnAccount.
  ///
  /// In en, this message translates to:
  /// **'Create an account'**
  String get createAnAccount;

  /// No description provided for @sendCode.
  ///
  /// In en, this message translates to:
  /// **'Send code'**
  String get sendCode;

  /// No description provided for @sixDigitCode.
  ///
  /// In en, this message translates to:
  /// **'6-digit code'**
  String get sixDigitCode;

  /// No description provided for @change.
  ///
  /// In en, this message translates to:
  /// **'Change'**
  String get change;

  /// No description provided for @yourName.
  ///
  /// In en, this message translates to:
  /// **'Your name'**
  String get yourName;

  /// No description provided for @venuesSeeName.
  ///
  /// In en, this message translates to:
  /// **'Venues see this name on your bookings.'**
  String get venuesSeeName;

  /// No description provided for @createAccount.
  ///
  /// In en, this message translates to:
  /// **'Create account'**
  String get createAccount;

  /// No description provided for @savePasswordSignIn.
  ///
  /// In en, this message translates to:
  /// **'Save password and sign in'**
  String get savePasswordSignIn;

  /// No description provided for @continueAction.
  ///
  /// In en, this message translates to:
  /// **'Continue'**
  String get continueAction;

  /// No description provided for @checking.
  ///
  /// In en, this message translates to:
  /// **'Checking…'**
  String get checking;

  /// No description provided for @resendCode.
  ///
  /// In en, this message translates to:
  /// **'Resend code'**
  String get resendCode;

  /// No description provided for @passwordTooShort.
  ///
  /// In en, this message translates to:
  /// **'Use at least {count} characters for your password.'**
  String passwordTooShort(int count);

  /// No description provided for @passwordHelp.
  ///
  /// In en, this message translates to:
  /// **'At least {count} characters. Not your phone number, or something as easy to guess.'**
  String passwordHelp(int count);

  /// The phone number stays in Latin digits
  ///
  /// In en, this message translates to:
  /// **'Sent to {phone} · '**
  String sentTo(String phone);

  /// No description provided for @resendIn.
  ///
  /// In en, this message translates to:
  /// **'Resend code in {seconds}s'**
  String resendIn(int seconds);

  /// No description provided for @meDeposit.
  ///
  /// In en, this message translates to:
  /// **'Booking deposit'**
  String get meDeposit;

  /// No description provided for @meDepositDetail.
  ///
  /// In en, this message translates to:
  /// **'Top up or take it back'**
  String get meDepositDetail;

  /// No description provided for @meProfile.
  ///
  /// In en, this message translates to:
  /// **'Player profile'**
  String get meProfile;

  /// No description provided for @meProfileDetail.
  ///
  /// In en, this message translates to:
  /// **'Your position and level, for games and teams'**
  String get meProfileDetail;

  /// No description provided for @meMemberships.
  ///
  /// In en, this message translates to:
  /// **'Memberships'**
  String get meMemberships;

  /// No description provided for @meMembershipsDetail.
  ///
  /// In en, this message translates to:
  /// **'Game packs and passes'**
  String get meMembershipsDetail;

  /// No description provided for @meDues.
  ///
  /// In en, this message translates to:
  /// **'What you owe'**
  String get meDues;

  /// No description provided for @meDuesDetail.
  ///
  /// In en, this message translates to:
  /// **'Your share of games friends booked'**
  String get meDuesDetail;

  /// No description provided for @meFavourites.
  ///
  /// In en, this message translates to:
  /// **'Saved venues'**
  String get meFavourites;

  /// No description provided for @meFavouritesDetail.
  ///
  /// In en, this message translates to:
  /// **'Venues you like, to book again'**
  String get meFavouritesDetail;

  /// No description provided for @meReviews.
  ///
  /// In en, this message translates to:
  /// **'Reviews'**
  String get meReviews;

  /// No description provided for @meReviewsDetail.
  ///
  /// In en, this message translates to:
  /// **'Rate the venues you played at'**
  String get meReviewsDetail;

  /// No description provided for @meWaitlist.
  ///
  /// In en, this message translates to:
  /// **'Waitlist'**
  String get meWaitlist;

  /// No description provided for @meWaitlistDetail.
  ///
  /// In en, this message translates to:
  /// **'Booked slots you want if they free up'**
  String get meWaitlistDetail;

  /// No description provided for @mePassword.
  ///
  /// In en, this message translates to:
  /// **'Password'**
  String get mePassword;

  /// No description provided for @mePasswordDetail.
  ///
  /// In en, this message translates to:
  /// **'Change it, and sign out other devices'**
  String get mePasswordDetail;

  /// No description provided for @saved.
  ///
  /// In en, this message translates to:
  /// **'Saved.'**
  String get saved;

  /// No description provided for @yourDetails.
  ///
  /// In en, this message translates to:
  /// **'Your details'**
  String get yourDetails;

  /// No description provided for @name.
  ///
  /// In en, this message translates to:
  /// **'Name'**
  String get name;

  /// No description provided for @venuesSeeThis.
  ///
  /// In en, this message translates to:
  /// **'Venues see this on your bookings.'**
  String get venuesSeeThis;

  /// No description provided for @email.
  ///
  /// In en, this message translates to:
  /// **'Email'**
  String get email;

  /// No description provided for @save.
  ///
  /// In en, this message translates to:
  /// **'Save'**
  String get save;

  /// No description provided for @appearance.
  ///
  /// In en, this message translates to:
  /// **'Appearance'**
  String get appearance;

  /// No description provided for @themeLight.
  ///
  /// In en, this message translates to:
  /// **'Light'**
  String get themeLight;

  /// No description provided for @themeDark.
  ///
  /// In en, this message translates to:
  /// **'Dark'**
  String get themeDark;

  /// No description provided for @themeAuto.
  ///
  /// In en, this message translates to:
  /// **'Auto'**
  String get themeAuto;

  /// No description provided for @signOut.
  ///
  /// In en, this message translates to:
  /// **'Sign out'**
  String get signOut;

  /// No description provided for @language.
  ///
  /// In en, this message translates to:
  /// **'Language'**
  String get language;

  /// No description provided for @languageHelp.
  ///
  /// In en, this message translates to:
  /// **'Notifications and texts come in it too.'**
  String get languageHelp;

  /// No description provided for @useAtLeast8.
  ///
  /// In en, this message translates to:
  /// **'Use at least 8 characters.'**
  String get useAtLeast8;

  /// No description provided for @enterCurrentPassword.
  ///
  /// In en, this message translates to:
  /// **'Enter your current password.'**
  String get enterCurrentPassword;

  /// No description provided for @passwordSaved.
  ///
  /// In en, this message translates to:
  /// **'Password saved. Other devices are signed out.'**
  String get passwordSaved;

  /// No description provided for @hidePasswords.
  ///
  /// In en, this message translates to:
  /// **'Hide passwords'**
  String get hidePasswords;

  /// No description provided for @showPasswords.
  ///
  /// In en, this message translates to:
  /// **'Show passwords'**
  String get showPasswords;

  /// No description provided for @setAPassword.
  ///
  /// In en, this message translates to:
  /// **'Set a password'**
  String get setAPassword;

  /// No description provided for @googleOnlyNote.
  ///
  /// In en, this message translates to:
  /// **'You sign in with Google. A password lets you sign in with your number too.'**
  String get googleOnlyNote;

  /// No description provided for @currentPassword.
  ///
  /// In en, this message translates to:
  /// **'Current password'**
  String get currentPassword;

  /// No description provided for @passwordStayHere.
  ///
  /// In en, this message translates to:
  /// **'At least 8 characters. You stay signed in here; other devices are signed out.'**
  String get passwordStayHere;

  /// No description provided for @changePassword.
  ///
  /// In en, this message translates to:
  /// **'Change password'**
  String get changePassword;

  /// No description provided for @setPassword.
  ///
  /// In en, this message translates to:
  /// **'Set password'**
  String get setPassword;

  /// No description provided for @exploreTitle.
  ///
  /// In en, this message translates to:
  /// **'Book a futsal court'**
  String get exploreTitle;

  /// No description provided for @exploreSubtitle.
  ///
  /// In en, this message translates to:
  /// **'Free slots and prices at venues near you. Pay the advance with eSewa or Khalti.'**
  String get exploreSubtitle;

  /// No description provided for @venueSearchHint.
  ///
  /// In en, this message translates to:
  /// **'Venue name or area'**
  String get venueSearchHint;

  /// No description provided for @clear.
  ///
  /// In en, this message translates to:
  /// **'Clear'**
  String get clear;

  /// No description provided for @allCities.
  ///
  /// In en, this message translates to:
  /// **'All cities'**
  String get allCities;

  /// No description provided for @featured.
  ///
  /// In en, this message translates to:
  /// **'Featured'**
  String get featured;

  /// No description provided for @allVenues.
  ///
  /// In en, this message translates to:
  /// **'All venues'**
  String get allVenues;

  /// No description provided for @noVenuesFound.
  ///
  /// In en, this message translates to:
  /// **'No venues found'**
  String get noVenuesFound;

  /// No description provided for @noVenuesFoundBody.
  ///
  /// In en, this message translates to:
  /// **'Try another name or area, or all cities.'**
  String get noVenuesFoundBody;

  /// No description provided for @loadingMore.
  ///
  /// In en, this message translates to:
  /// **'Loading…'**
  String get loadingMore;

  /// No description provided for @showMoreVenues.
  ///
  /// In en, this message translates to:
  /// **'Show more venues'**
  String get showMoreVenues;

  /// No description provided for @waitBody.
  ///
  /// In en, this message translates to:
  /// **'Wait for it, and we\'ll tell you if it frees up. Whoever books it first then gets it.'**
  String get waitBody;

  /// No description provided for @waitForIt.
  ///
  /// In en, this message translates to:
  /// **'Wait for it'**
  String get waitForIt;

  /// No description provided for @saved_.
  ///
  /// In en, this message translates to:
  /// **'Saved'**
  String get saved_;

  /// No description provided for @saveVenue.
  ///
  /// In en, this message translates to:
  /// **'Save'**
  String get saveVenue;

  /// No description provided for @message.
  ///
  /// In en, this message translates to:
  /// **'Message'**
  String get message;

  /// No description provided for @bookASlot.
  ///
  /// In en, this message translates to:
  /// **'Book a slot'**
  String get bookASlot;

  /// No description provided for @bookingRules.
  ///
  /// In en, this message translates to:
  /// **'Booking rules'**
  String get bookingRules;

  /// No description provided for @amenities.
  ///
  /// In en, this message translates to:
  /// **'Amenities'**
  String get amenities;

  /// No description provided for @reviewsTitle.
  ///
  /// In en, this message translates to:
  /// **'Reviews'**
  String get reviewsTitle;

  /// No description provided for @noReviewsYet.
  ///
  /// In en, this message translates to:
  /// **'No reviews yet. Players can review after they play here.'**
  String get noReviewsYet;

  /// No description provided for @noCourtsYet.
  ///
  /// In en, this message translates to:
  /// **'No courts are open for booking yet.'**
  String get noCourtsYet;

  /// No description provided for @closedThisDay.
  ///
  /// In en, this message translates to:
  /// **'Closed this day.'**
  String get closedThisDay;

  /// No description provided for @noMoreGamesToday.
  ///
  /// In en, this message translates to:
  /// **'No more games today.'**
  String get noMoreGamesToday;

  /// No description provided for @today.
  ///
  /// In en, this message translates to:
  /// **'Today'**
  String get today;

  /// No description provided for @tomorrow.
  ///
  /// In en, this message translates to:
  /// **'Tomorrow'**
  String get tomorrow;

  /// No description provided for @fullyBooked.
  ///
  /// In en, this message translates to:
  /// **'Fully booked'**
  String get fullyBooked;

  /// No description provided for @waiting.
  ///
  /// In en, this message translates to:
  /// **'Waiting'**
  String get waiting;

  /// No description provided for @booked.
  ///
  /// In en, this message translates to:
  /// **'Booked'**
  String get booked;

  /// No description provided for @openingHours.
  ///
  /// In en, this message translates to:
  /// **'Opening hours'**
  String get openingHours;

  /// No description provided for @memberships.
  ///
  /// In en, this message translates to:
  /// **'Memberships'**
  String get memberships;

  /// No description provided for @membershipsPitch.
  ///
  /// In en, this message translates to:
  /// **'Play more, pay less. Use it when you book here.'**
  String get membershipsPitch;

  /// No description provided for @free.
  ///
  /// In en, this message translates to:
  /// **'Free'**
  String get free;

  /// No description provided for @buy.
  ///
  /// In en, this message translates to:
  /// **'Buy'**
  String get buy;

  /// No description provided for @iPaid.
  ///
  /// In en, this message translates to:
  /// **'I\'ve paid'**
  String get iPaid;

  /// No description provided for @getIt.
  ///
  /// In en, this message translates to:
  /// **'Get it'**
  String get getIt;

  /// No description provided for @gettingIt.
  ///
  /// In en, this message translates to:
  /// **'Getting it…'**
  String get gettingIt;

  /// No description provided for @notNow.
  ///
  /// In en, this message translates to:
  /// **'Not now'**
  String get notNow;

  /// No description provided for @venuesFound.
  ///
  /// In en, this message translates to:
  /// **'{count, plural, =1{1 venue} other{{count} venues}}'**
  String venuesFound(int count);

  /// No description provided for @slotBookedOn.
  ///
  /// In en, this message translates to:
  /// **'{time} on {court} is booked'**
  String slotBookedOn(String time, String court);

  /// No description provided for @weWillTellYou.
  ///
  /// In en, this message translates to:
  /// **'We\'ll tell you if {time} frees up.'**
  String weWillTellYou(String time);

  /// No description provided for @freeSlots.
  ///
  /// In en, this message translates to:
  /// **'{count} free'**
  String freeSlots(int count);

  /// No description provided for @slotWaiting.
  ///
  /// In en, this message translates to:
  /// **'{time}, booked. You\'re waiting for it'**
  String slotWaiting(String time);

  /// No description provided for @slotWaitQuestion.
  ///
  /// In en, this message translates to:
  /// **'{time} is booked. Wait for it?'**
  String slotWaitQuestion(String time);

  /// No description provided for @slotClosed.
  ///
  /// In en, this message translates to:
  /// **'{time}, closed'**
  String slotClosed(String time);

  /// No description provided for @slotBook.
  ///
  /// In en, this message translates to:
  /// **'{time}, {price}. Book'**
  String slotBook(String time, String price);

  /// No description provided for @playedOn.
  ///
  /// In en, this message translates to:
  /// **'Played {date}'**
  String playedOn(String date);

  /// No description provided for @venueReplied.
  ///
  /// In en, this message translates to:
  /// **'{venue} replied'**
  String venueReplied(String venue);

  /// No description provided for @membershipChecks.
  ///
  /// In en, this message translates to:
  /// **'{venue} checks your payment, then your membership starts.'**
  String membershipChecks(String venue);

  /// No description provided for @bookThisSlot.
  ///
  /// In en, this message translates to:
  /// **'Book this slot'**
  String get bookThisSlot;

  /// No description provided for @slotUnavailable.
  ///
  /// In en, this message translates to:
  /// **'This slot can\'t be booked'**
  String get slotUnavailable;

  /// No description provided for @seeOtherSlots.
  ///
  /// In en, this message translates to:
  /// **'See other free slots'**
  String get seeOtherSlots;

  /// No description provided for @addDepositFirst.
  ///
  /// In en, this message translates to:
  /// **'Add your booking deposit first'**
  String get addDepositFirst;

  /// No description provided for @thisVenue.
  ///
  /// In en, this message translates to:
  /// **'This venue'**
  String get thisVenue;

  /// No description provided for @extras.
  ///
  /// In en, this message translates to:
  /// **'Extras'**
  String get extras;

  /// No description provided for @extrasPaidAtVenue.
  ///
  /// In en, this message translates to:
  /// **'Paid at the venue with your booking.'**
  String get extrasPaidAtVenue;

  /// No description provided for @useMembership.
  ///
  /// In en, this message translates to:
  /// **'Use a membership'**
  String get useMembership;

  /// No description provided for @dontUseOne.
  ///
  /// In en, this message translates to:
  /// **'Don\'t use one'**
  String get dontUseOne;

  /// No description provided for @discountCode.
  ///
  /// In en, this message translates to:
  /// **'Discount code'**
  String get discountCode;

  /// No description provided for @remove.
  ///
  /// In en, this message translates to:
  /// **'Remove'**
  String get remove;

  /// No description provided for @codeHint.
  ///
  /// In en, this message translates to:
  /// **'e.g. DASHAIN20'**
  String get codeHint;

  /// No description provided for @apply.
  ///
  /// In en, this message translates to:
  /// **'Apply'**
  String get apply;

  /// No description provided for @noteForVenue.
  ///
  /// In en, this message translates to:
  /// **'Note for the venue'**
  String get noteForVenue;

  /// No description provided for @noteHint.
  ///
  /// In en, this message translates to:
  /// **'e.g. We\'re 10 players, need bibs'**
  String get noteHint;

  /// No description provided for @workingOutPrice.
  ///
  /// In en, this message translates to:
  /// **'Working out the price'**
  String get workingOutPrice;

  /// No description provided for @court.
  ///
  /// In en, this message translates to:
  /// **'Court'**
  String get court;

  /// No description provided for @discount.
  ///
  /// In en, this message translates to:
  /// **'Discount'**
  String get discount;

  /// No description provided for @total.
  ///
  /// In en, this message translates to:
  /// **'Total'**
  String get total;

  /// No description provided for @payNowToConfirm.
  ///
  /// In en, this message translates to:
  /// **'Pay now to confirm'**
  String get payNowToConfirm;

  /// No description provided for @atTheVenue.
  ///
  /// In en, this message translates to:
  /// **'At the venue'**
  String get atTheVenue;

  /// No description provided for @pay.
  ///
  /// In en, this message translates to:
  /// **'Pay'**
  String get pay;

  /// No description provided for @strikeAdvanceNote.
  ///
  /// In en, this message translates to:
  /// **'You pay more up front here because of your recent late cancellations or no-shows at this venue.'**
  String get strikeAdvanceNote;

  /// No description provided for @book.
  ///
  /// In en, this message translates to:
  /// **'Book'**
  String get book;

  /// No description provided for @booking_.
  ///
  /// In en, this message translates to:
  /// **'Booking…'**
  String get booking_;

  /// No description provided for @depositExplain.
  ///
  /// In en, this message translates to:
  /// **'{venue} asks players to keep {amount} with FutSlot. It isn\'t spent on your games: it only pays the venue\'s fee if you cancel late or don\'t show up, and you can take it back when you have no games booked.'**
  String depositExplain(String venue, String amount);

  /// No description provided for @addAmount.
  ///
  /// In en, this message translates to:
  /// **'Add {amount}'**
  String addAmount(String amount);

  /// No description provided for @gamesLeft.
  ///
  /// In en, this message translates to:
  /// **'{count} left'**
  String gamesLeft(int count);

  /// No description provided for @bookAndPay.
  ///
  /// In en, this message translates to:
  /// **'Book and pay {amount}'**
  String bookAndPay(String amount);

  /// No description provided for @holdWhilePay.
  ///
  /// In en, this message translates to:
  /// **'We hold the slot for {minutes} minutes while you pay the venue.'**
  String holdWhilePay(int minutes);

  /// No description provided for @priceEach.
  ///
  /// In en, this message translates to:
  /// **'{price} each'**
  String priceEach(String price);

  /// No description provided for @oneLess.
  ///
  /// In en, this message translates to:
  /// **'One less {item}'**
  String oneLess(String item);

  /// No description provided for @oneMore.
  ///
  /// In en, this message translates to:
  /// **'One more {item}'**
  String oneMore(String item);

  /// No description provided for @yourBookings.
  ///
  /// In en, this message translates to:
  /// **'Your bookings'**
  String get yourBookings;

  /// No description provided for @upcoming.
  ///
  /// In en, this message translates to:
  /// **'Upcoming'**
  String get upcoming;

  /// No description provided for @past.
  ///
  /// In en, this message translates to:
  /// **'Past'**
  String get past;

  /// No description provided for @noGamesComingUp.
  ///
  /// In en, this message translates to:
  /// **'No games coming up'**
  String get noGamesComingUp;

  /// No description provided for @nothingHereYet.
  ///
  /// In en, this message translates to:
  /// **'Nothing here yet'**
  String get nothingHereYet;

  /// No description provided for @findCourtPitch.
  ///
  /// In en, this message translates to:
  /// **'Find a free court and book it in a minute.'**
  String get findCourtPitch;

  /// No description provided for @pastGamesShowHere.
  ///
  /// In en, this message translates to:
  /// **'Games you play or cancel show up here.'**
  String get pastGamesShowHere;

  /// No description provided for @showMore.
  ///
  /// In en, this message translates to:
  /// **'Show more'**
  String get showMore;

  /// No description provided for @booking.
  ///
  /// In en, this message translates to:
  /// **'Booking'**
  String get booking;

  /// No description provided for @bookedSeeYou.
  ///
  /// In en, this message translates to:
  /// **'Booked. See you on the court!'**
  String get bookedSeeYou;

  /// No description provided for @bookedPayBelow.
  ///
  /// In en, this message translates to:
  /// **'Booked. Pay the advance below to confirm it.'**
  String get bookedPayBelow;

  /// No description provided for @messageTheVenue.
  ///
  /// In en, this message translates to:
  /// **'Message the venue'**
  String get messageTheVenue;

  /// No description provided for @splitTheCost.
  ///
  /// In en, this message translates to:
  /// **'Split the cost'**
  String get splitTheCost;

  /// No description provided for @splitTheCostDetail.
  ///
  /// In en, this message translates to:
  /// **'Share it equally, or baazi: the losers pay'**
  String get splitTheCostDetail;

  /// No description provided for @yourNote.
  ///
  /// In en, this message translates to:
  /// **'Your note'**
  String get yourNote;

  /// No description provided for @cancelled.
  ///
  /// In en, this message translates to:
  /// **'Cancelled'**
  String get cancelled;

  /// No description provided for @cancelledRefund.
  ///
  /// In en, this message translates to:
  /// **'Anything you paid comes back to you.'**
  String get cancelledRefund;

  /// No description provided for @yourReview.
  ///
  /// In en, this message translates to:
  /// **'Your review'**
  String get yourReview;

  /// No description provided for @edit.
  ///
  /// In en, this message translates to:
  /// **'Edit'**
  String get edit;

  /// No description provided for @delete.
  ///
  /// In en, this message translates to:
  /// **'Delete'**
  String get delete;

  /// No description provided for @editYourReview.
  ///
  /// In en, this message translates to:
  /// **'Edit your review'**
  String get editYourReview;

  /// No description provided for @reviewShownAs.
  ///
  /// In en, this message translates to:
  /// **'Other players see it with your first name and last initial.'**
  String get reviewShownAs;

  /// No description provided for @cancel.
  ///
  /// In en, this message translates to:
  /// **'Cancel'**
  String get cancel;

  /// No description provided for @paymentSent.
  ///
  /// In en, this message translates to:
  /// **'Payment sent'**
  String get paymentSent;

  /// No description provided for @payDirectly.
  ///
  /// In en, this message translates to:
  /// **'Pay the venue directly, then tell us the transaction ID. The venue checks it and confirms your booking.'**
  String get payDirectly;

  /// No description provided for @timeToPayRanOut.
  ///
  /// In en, this message translates to:
  /// **'The time to pay has run out'**
  String get timeToPayRanOut;

  /// No description provided for @slotMayBeGone.
  ///
  /// In en, this message translates to:
  /// **'The slot may have gone to someone else. Book again if it\'s still free.'**
  String get slotMayBeGone;

  /// No description provided for @seeFreeSlots.
  ///
  /// In en, this message translates to:
  /// **'See free slots'**
  String get seeFreeSlots;

  /// No description provided for @payment.
  ///
  /// In en, this message translates to:
  /// **'Payment'**
  String get payment;

  /// No description provided for @paid.
  ///
  /// In en, this message translates to:
  /// **'Paid'**
  String get paid;

  /// No description provided for @beingChecked.
  ///
  /// In en, this message translates to:
  /// **' · being checked'**
  String get beingChecked;

  /// No description provided for @started.
  ///
  /// In en, this message translates to:
  /// **' · started'**
  String get started;

  /// No description provided for @notReceived.
  ///
  /// In en, this message translates to:
  /// **' · not received'**
  String get notReceived;

  /// No description provided for @paymentCancelled.
  ///
  /// In en, this message translates to:
  /// **' · cancelled'**
  String get paymentCancelled;

  /// No description provided for @refunded.
  ///
  /// In en, this message translates to:
  /// **' · refunded'**
  String get refunded;

  /// No description provided for @partlyRefunded.
  ///
  /// In en, this message translates to:
  /// **' · partly refunded'**
  String get partlyRefunded;

  /// No description provided for @cancelBooking.
  ///
  /// In en, this message translates to:
  /// **'Cancel booking'**
  String get cancelBooking;

  /// No description provided for @cancelThisBooking.
  ///
  /// In en, this message translates to:
  /// **'Cancel this booking?'**
  String get cancelThisBooking;

  /// No description provided for @why.
  ///
  /// In en, this message translates to:
  /// **'Why?'**
  String get why;

  /// No description provided for @cancelling.
  ///
  /// In en, this message translates to:
  /// **'Cancelling…'**
  String get cancelling;

  /// No description provided for @keepIt.
  ///
  /// In en, this message translates to:
  /// **'Keep it'**
  String get keepIt;

  /// No description provided for @freeToCancel.
  ///
  /// In en, this message translates to:
  /// **'It\'s free to cancel now.'**
  String get freeToCancel;

  /// No description provided for @bookingRef.
  ///
  /// In en, this message translates to:
  /// **'Booking {reference}'**
  String bookingRef(String reference);

  /// No description provided for @timeRange.
  ///
  /// In en, this message translates to:
  /// **'{start} – {end}'**
  String timeRange(String start, String end);

  /// No description provided for @bookingSummaryLabel.
  ///
  /// In en, this message translates to:
  /// **'{day}, {start} to {end}, {court}'**
  String bookingSummaryLabel(
    String day,
    String start,
    String end,
    String court,
  );

  /// No description provided for @cancelledLate.
  ///
  /// In en, this message translates to:
  /// **'Cancelled late, so the venue kept its {percent}% fee.'**
  String cancelledLate(String percent);

  /// No description provided for @cancelledLateDeposit.
  ///
  /// In en, this message translates to:
  /// **'Cancelled late, so the venue kept its {percent}% fee, with any part your advance didn\'t cover taken from your deposit.'**
  String cancelledLateDeposit(String percent);

  /// No description provided for @howWasVenue.
  ///
  /// In en, this message translates to:
  /// **'How was {venue}?'**
  String howWasVenue(String venue);

  /// No description provided for @venueChecking.
  ///
  /// In en, this message translates to:
  /// **'{venue} is checking it. Your slot is held meanwhile, and we\'ll let you know when it\'s confirmed.'**
  String venueChecking(String venue);

  /// No description provided for @payToConfirm.
  ///
  /// In en, this message translates to:
  /// **'Pay {amount} to confirm'**
  String payToConfirm(String amount);

  /// No description provided for @heldFor.
  ///
  /// In en, this message translates to:
  /// **'{count, plural, =1{Held for 1 more minute, until {time}.} other{Held for {count} more minutes, until {time}.}}'**
  String heldFor(int count, String time);

  /// No description provided for @lineItem.
  ///
  /// In en, this message translates to:
  /// **'{quantity} × {name}'**
  String lineItem(int quantity, String name);

  /// No description provided for @refundBy.
  ///
  /// In en, this message translates to:
  /// **'Refund by {method} · {state}'**
  String refundBy(String method, String state);

  /// No description provided for @refundSent.
  ///
  /// In en, this message translates to:
  /// **'sent'**
  String get refundSent;

  /// No description provided for @refundOnItsWay.
  ///
  /// In en, this message translates to:
  /// **'on its way'**
  String get refundOnItsWay;

  /// No description provided for @youGetBack.
  ///
  /// In en, this message translates to:
  /// **'You get {amount} back.'**
  String youGetBack(String amount);

  /// No description provided for @cancelCosts.
  ///
  /// In en, this message translates to:
  /// **'Cancelling now costs {percent}% of the price ({fee}).'**
  String cancelCosts(String percent, String fee);

  /// No description provided for @venueKeeps.
  ///
  /// In en, this message translates to:
  /// **'The venue keeps {amount} of what you paid.'**
  String venueKeeps(String amount);

  /// No description provided for @venueKeepsRefund.
  ///
  /// In en, this message translates to:
  /// **'The venue keeps {amount} of what you paid and you get {refund} back.'**
  String venueKeepsRefund(String amount, String refund);

  /// No description provided for @fromDeposit.
  ///
  /// In en, this message translates to:
  /// **'Up to {amount} more comes out of your FutSlot deposit.'**
  String fromDeposit(String amount);

  /// No description provided for @bankAccount.
  ///
  /// In en, this message translates to:
  /// **'Bank account'**
  String get bankAccount;

  /// No description provided for @enterSendTo.
  ///
  /// In en, this message translates to:
  /// **'Enter the number or account to send it to.'**
  String get enterSendTo;

  /// No description provided for @toppedUp.
  ///
  /// In en, this message translates to:
  /// **'Topped up'**
  String get toppedUp;

  /// No description provided for @takenBack.
  ///
  /// In en, this message translates to:
  /// **'Taken back'**
  String get takenBack;

  /// No description provided for @keptAfterAll.
  ///
  /// In en, this message translates to:
  /// **'Kept after all'**
  String get keptAfterAll;

  /// No description provided for @bookingDeposit.
  ///
  /// In en, this message translates to:
  /// **'Booking deposit'**
  String get bookingDeposit;

  /// No description provided for @inYourDeposit.
  ///
  /// In en, this message translates to:
  /// **'In your deposit'**
  String get inYourDeposit;

  /// No description provided for @deposit.
  ///
  /// In en, this message translates to:
  /// **'Deposit'**
  String get deposit;

  /// No description provided for @depositFull.
  ///
  /// In en, this message translates to:
  /// **'Full: you can book any venue.'**
  String get depositFull;

  /// No description provided for @backToBooking.
  ///
  /// In en, this message translates to:
  /// **'Back to your booking'**
  String get backToBooking;

  /// No description provided for @keepDeposit.
  ///
  /// In en, this message translates to:
  /// **'Keep my deposit instead'**
  String get keepDeposit;

  /// No description provided for @topUpSent.
  ///
  /// In en, this message translates to:
  /// **'Top-up sent'**
  String get topUpSent;

  /// No description provided for @topUpChecking.
  ///
  /// In en, this message translates to:
  /// **'FutSlot is checking it, usually within a few hours. You can book once it\'s in.'**
  String get topUpChecking;

  /// No description provided for @payFutSlotDirectly.
  ///
  /// In en, this message translates to:
  /// **'Pay FutSlot directly, then tell us the transaction ID.'**
  String get payFutSlotDirectly;

  /// No description provided for @history.
  ///
  /// In en, this message translates to:
  /// **'History'**
  String get history;

  /// No description provided for @takeDepositBack.
  ///
  /// In en, this message translates to:
  /// **'Take your deposit back'**
  String get takeDepositBack;

  /// No description provided for @takeItBack.
  ///
  /// In en, this message translates to:
  /// **'Take it back'**
  String get takeItBack;

  /// No description provided for @sendItToMy.
  ///
  /// In en, this message translates to:
  /// **'Send it to my'**
  String get sendItToMy;

  /// No description provided for @bankNameAccount.
  ///
  /// In en, this message translates to:
  /// **'Bank name and account number'**
  String get bankNameAccount;

  /// No description provided for @number.
  ///
  /// In en, this message translates to:
  /// **'Number'**
  String get number;

  /// No description provided for @asking.
  ///
  /// In en, this message translates to:
  /// **'Asking…'**
  String get asking;

  /// No description provided for @askForItBack.
  ///
  /// In en, this message translates to:
  /// **'Ask for it back'**
  String get askForItBack;

  /// No description provided for @back.
  ///
  /// In en, this message translates to:
  /// **'Back'**
  String get back;

  /// No description provided for @membershipsIntro.
  ///
  /// In en, this message translates to:
  /// **'Game packs and passes you bought at venues. They\'re used when you book there.'**
  String get membershipsIntro;

  /// No description provided for @noMembershipsYet.
  ///
  /// In en, this message translates to:
  /// **'No memberships yet'**
  String get noMembershipsYet;

  /// No description provided for @noMembershipsBody.
  ///
  /// In en, this message translates to:
  /// **'Venues that sell game packs or passes list them on their page.'**
  String get noMembershipsBody;

  /// No description provided for @active.
  ///
  /// In en, this message translates to:
  /// **'Active'**
  String get active;

  /// No description provided for @expired.
  ///
  /// In en, this message translates to:
  /// **'Expired'**
  String get expired;

  /// No description provided for @savedVenues.
  ///
  /// In en, this message translates to:
  /// **'Saved venues'**
  String get savedVenues;

  /// No description provided for @noSavedVenues.
  ///
  /// In en, this message translates to:
  /// **'No saved venues'**
  String get noSavedVenues;

  /// No description provided for @noSavedVenuesBody.
  ///
  /// In en, this message translates to:
  /// **'Tap the heart on a venue you like to find it here.'**
  String get noSavedVenuesBody;

  /// No description provided for @howWasYourGame.
  ///
  /// In en, this message translates to:
  /// **'How was your game?'**
  String get howWasYourGame;

  /// No description provided for @reviewWindow.
  ///
  /// In en, this message translates to:
  /// **'You can review a game for 30 days after playing it.'**
  String get reviewWindow;

  /// No description provided for @review.
  ///
  /// In en, this message translates to:
  /// **'Review'**
  String get review;

  /// No description provided for @yourReviews.
  ///
  /// In en, this message translates to:
  /// **'Your reviews'**
  String get yourReviews;

  /// No description provided for @noReviewsWritten.
  ///
  /// In en, this message translates to:
  /// **'You haven\'t reviewed a venue yet.'**
  String get noReviewsWritten;

  /// No description provided for @reviewHidden.
  ///
  /// In en, this message translates to:
  /// **'Hidden by FutSlot while it checks it'**
  String get reviewHidden;

  /// No description provided for @reviewRemoved.
  ///
  /// In en, this message translates to:
  /// **'Removed by FutSlot'**
  String get reviewRemoved;

  /// No description provided for @waitlist.
  ///
  /// In en, this message translates to:
  /// **'Waitlist'**
  String get waitlist;

  /// No description provided for @waitlistIntro.
  ///
  /// In en, this message translates to:
  /// **'Slots you want that someone else booked. If one frees up we tell you, and whoever books it first gets it.'**
  String get waitlistIntro;

  /// No description provided for @notWaiting.
  ///
  /// In en, this message translates to:
  /// **'You\'re not waiting for any slot'**
  String get notWaiting;

  /// No description provided for @notWaitingBody.
  ///
  /// In en, this message translates to:
  /// **'On a venue\'s page, tap a booked slot to wait for it.'**
  String get notWaitingBody;

  /// No description provided for @itFreedUp.
  ///
  /// In en, this message translates to:
  /// **'It freed up. Book it before someone else does.'**
  String get itFreedUp;

  /// No description provided for @stopWaiting.
  ///
  /// In en, this message translates to:
  /// **'Stop waiting'**
  String get stopWaiting;

  /// No description provided for @markAllRead.
  ///
  /// In en, this message translates to:
  /// **'Mark all read'**
  String get markAllRead;

  /// No description provided for @nothingYet.
  ///
  /// In en, this message translates to:
  /// **'Nothing yet'**
  String get nothingYet;

  /// No description provided for @notificationsEmpty.
  ///
  /// In en, this message translates to:
  /// **'Booking confirmations, reminders and replies from venues show up here.'**
  String get notificationsEmpty;

  /// No description provided for @showOlder.
  ///
  /// In en, this message translates to:
  /// **'Show older'**
  String get showOlder;

  /// No description provided for @unread.
  ///
  /// In en, this message translates to:
  /// **'Unread'**
  String get unread;

  /// No description provided for @feeToVenue.
  ///
  /// In en, this message translates to:
  /// **'Fee to {venue} · {reference}'**
  String feeToVenue(String venue, String reference);

  /// No description provided for @depositIntro.
  ///
  /// In en, this message translates to:
  /// **'Some venues only take bookings from players who keep {amount} with FutSlot. It isn\'t spent on your games: it only pays a venue\'s fee if you cancel late or don\'t show up. You can take it back whenever you have no games booked there.'**
  String depositIntro(String amount);

  /// No description provided for @ofAmount.
  ///
  /// In en, this message translates to:
  /// **' of {amount}'**
  String ofAmount(String amount);

  /// No description provided for @comingBack.
  ///
  /// In en, this message translates to:
  /// **'{amount} is coming back to you'**
  String comingBack(String amount);

  /// No description provided for @withdrawalWhen.
  ///
  /// In en, this message translates to:
  /// **'FutSlot sends it to your {method} {sendTo} on {date}. Until then you can\'t book venues that need the deposit.'**
  String withdrawalWhen(String method, String sendTo, String date);

  /// No description provided for @depositInUse.
  ///
  /// In en, this message translates to:
  /// **'Your deposit is covering a game'**
  String get depositInUse;

  /// No description provided for @depositInUseWhen.
  ///
  /// In en, this message translates to:
  /// **'You can take it back once the games you\'ve booked at venues that need it are played or cancelled.'**
  String get depositInUseWhen;

  /// No description provided for @withdrawExplain.
  ///
  /// In en, this message translates to:
  /// **'FutSlot sends all of {amount} to you 3 days after you ask.'**
  String withdrawExplain(String amount);

  /// No description provided for @until.
  ///
  /// In en, this message translates to:
  /// **'Until {date}'**
  String until(String date);

  /// No description provided for @ended.
  ///
  /// In en, this message translates to:
  /// **'Ended {date}'**
  String ended(String date);

  /// No description provided for @messages.
  ///
  /// In en, this message translates to:
  /// **'Messages'**
  String get messages;

  /// No description provided for @findAVenue.
  ///
  /// In en, this message translates to:
  /// **'Find a venue'**
  String get findAVenue;

  /// No description provided for @noMessagesYet.
  ///
  /// In en, this message translates to:
  /// **'No messages yet'**
  String get noMessagesYet;

  /// No description provided for @noMessagesBody.
  ///
  /// In en, this message translates to:
  /// **'Ask a venue about courts, a booking or anything else: tap Message on its page or on your booking.'**
  String get noMessagesBody;

  /// No description provided for @typeMessageFirst.
  ///
  /// In en, this message translates to:
  /// **'Type a message first.'**
  String get typeMessageFirst;

  /// No description provided for @venue.
  ///
  /// In en, this message translates to:
  /// **'Venue'**
  String get venue;

  /// No description provided for @showEarlier.
  ///
  /// In en, this message translates to:
  /// **'Show earlier messages'**
  String get showEarlier;

  /// No description provided for @send.
  ///
  /// In en, this message translates to:
  /// **'Send'**
  String get send;

  /// No description provided for @seen.
  ///
  /// In en, this message translates to:
  /// **' · Seen'**
  String get seen;

  /// No description provided for @yesterday.
  ///
  /// In en, this message translates to:
  /// **'Yesterday'**
  String get yesterday;

  /// No description provided for @nobodyMatches.
  ///
  /// In en, this message translates to:
  /// **'Nobody matches \"{text}\".'**
  String nobodyMatches(String text);

  /// No description provided for @unreadCount.
  ///
  /// In en, this message translates to:
  /// **'{count} unread'**
  String unreadCount(int count);

  /// No description provided for @askVenueBooking.
  ///
  /// In en, this message translates to:
  /// **'Ask {venue} about courts, prices or this booking. They reply here, and we\'ll let you know.'**
  String askVenueBooking(String venue);

  /// No description provided for @askVenue.
  ///
  /// In en, this message translates to:
  /// **'Ask {venue} about courts, prices or a booking. They reply here, and we\'ll let you know.'**
  String askVenue(String venue);

  /// No description provided for @messageTo.
  ///
  /// In en, this message translates to:
  /// **'Message {venue}'**
  String messageTo(String venue);

  /// No description provided for @giveNameOrPhone.
  ///
  /// In en, this message translates to:
  /// **'Give their name, or the phone number they use FutSlot with.'**
  String get giveNameOrPhone;

  /// No description provided for @changeSplit.
  ///
  /// In en, this message translates to:
  /// **'Change how the cost is shared.'**
  String get changeSplit;

  /// No description provided for @startSplitIntro.
  ///
  /// In en, this message translates to:
  /// **'Share what this game costs with the players: equally, or baazi-style.'**
  String get startSplitIntro;

  /// No description provided for @whoPays.
  ///
  /// In en, this message translates to:
  /// **'Who pays'**
  String get whoPays;

  /// No description provided for @losingSidePays.
  ///
  /// In en, this message translates to:
  /// **'The losing side pays'**
  String get losingSidePays;

  /// No description provided for @percentOfEverything.
  ///
  /// In en, this message translates to:
  /// **'% of everything'**
  String get percentOfEverything;

  /// No description provided for @oneSide.
  ///
  /// In en, this message translates to:
  /// **'One side'**
  String get oneSide;

  /// No description provided for @otherSide.
  ///
  /// In en, this message translates to:
  /// **'The other side'**
  String get otherSide;

  /// No description provided for @teamA.
  ///
  /// In en, this message translates to:
  /// **'Team A'**
  String get teamA;

  /// No description provided for @teamB.
  ///
  /// In en, this message translates to:
  /// **'Team B'**
  String get teamB;

  /// No description provided for @youPlayFor.
  ///
  /// In en, this message translates to:
  /// **'You play for'**
  String get youPlayFor;

  /// No description provided for @extrasRs.
  ///
  /// In en, this message translates to:
  /// **'Extras (Rs)'**
  String get extrasRs;

  /// No description provided for @forWhat.
  ///
  /// In en, this message translates to:
  /// **'For'**
  String get forWhat;

  /// No description provided for @extrasHint.
  ///
  /// In en, this message translates to:
  /// **'Water, drinks…'**
  String get extrasHint;

  /// No description provided for @startSplitting.
  ///
  /// In en, this message translates to:
  /// **'Start splitting'**
  String get startSplitting;

  /// No description provided for @played.
  ///
  /// In en, this message translates to:
  /// **'Played'**
  String get played;

  /// No description provided for @undo.
  ///
  /// In en, this message translates to:
  /// **'Undo'**
  String get undo;

  /// No description provided for @playedTheGame.
  ///
  /// In en, this message translates to:
  /// **'Played the game?'**
  String get playedTheGame;

  /// No description provided for @whoWon.
  ///
  /// In en, this message translates to:
  /// **'Who won?'**
  String get whoWon;

  /// No description provided for @worksOutShares.
  ///
  /// In en, this message translates to:
  /// **'This works out everyone\'s share and tells the players with the app.'**
  String get worksOutShares;

  /// No description provided for @workOutShares.
  ///
  /// In en, this message translates to:
  /// **'Work out shares'**
  String get workOutShares;

  /// No description provided for @addAPlayer.
  ///
  /// In en, this message translates to:
  /// **'Add a player'**
  String get addAPlayer;

  /// No description provided for @addPlayerHelp.
  ///
  /// In en, this message translates to:
  /// **'By the phone number they use FutSlot with, so they see their share, or just a name.'**
  String get addPlayerHelp;

  /// No description provided for @phone.
  ///
  /// In en, this message translates to:
  /// **'Phone'**
  String get phone;

  /// No description provided for @add.
  ///
  /// In en, this message translates to:
  /// **'Add'**
  String get add;

  /// No description provided for @eachPaysRoughly.
  ///
  /// In en, this message translates to:
  /// **'Each player pays, roughly'**
  String get eachPaysRoughly;

  /// No description provided for @ifResult.
  ///
  /// In en, this message translates to:
  /// **'If'**
  String get ifResult;

  /// No description provided for @you.
  ///
  /// In en, this message translates to:
  /// **' (you)'**
  String get you;

  /// No description provided for @bookedBy.
  ///
  /// In en, this message translates to:
  /// **' (booked)'**
  String get bookedBy;

  /// No description provided for @whatYouOwe.
  ///
  /// In en, this message translates to:
  /// **'What you owe'**
  String get whatYouOwe;

  /// No description provided for @duesIntro.
  ///
  /// In en, this message translates to:
  /// **'Your share of games other players booked. Pay them, and they tick you off.'**
  String get duesIntro;

  /// No description provided for @allSquare.
  ///
  /// In en, this message translates to:
  /// **'You\'re all square'**
  String get allSquare;

  /// No description provided for @allSquareBody.
  ///
  /// In en, this message translates to:
  /// **'When someone adds you to a game they booked and splits the cost, your share shows up here.'**
  String get allSquareBody;

  /// No description provided for @inAll.
  ///
  /// In en, this message translates to:
  /// **' in all'**
  String get inAll;

  /// No description provided for @extrasFor.
  ///
  /// In en, this message translates to:
  /// **'Extras · {note}'**
  String extrasFor(String note);

  /// No description provided for @collectedSoFar.
  ///
  /// In en, this message translates to:
  /// **'{collected} collected · {outstanding} still to come'**
  String collectedSoFar(String collected, String outstanding);

  /// No description provided for @shareOnceResult.
  ///
  /// In en, this message translates to:
  /// **'Your share is worked out once {name} records the result.'**
  String shareOnceResult(String name);

  /// No description provided for @sharePaid.
  ///
  /// In en, this message translates to:
  /// **'Paid. {name} has your {amount}.'**
  String sharePaid(String name, String amount);

  /// No description provided for @youOwe.
  ///
  /// In en, this message translates to:
  /// **'You owe {name} {amount}'**
  String youOwe(String name, String amount);

  /// No description provided for @payThemOn.
  ///
  /// In en, this message translates to:
  /// **'Pay them on eSewa or Khalti: {phone}. They tick you off once it\'s in.'**
  String payThemOn(String phone);

  /// No description provided for @paidName.
  ///
  /// In en, this message translates to:
  /// **'Paid: {name}'**
  String paidName(String name);

  /// No description provided for @markPaidName.
  ///
  /// In en, this message translates to:
  /// **'Mark paid: {name}'**
  String markPaidName(String name);

  /// No description provided for @moveToOtherSide.
  ///
  /// In en, this message translates to:
  /// **'Move {name} to the other side'**
  String moveToOtherSide(String name);

  /// No description provided for @removeName.
  ///
  /// In en, this message translates to:
  /// **'Remove {name}'**
  String removeName(String name);

  /// No description provided for @payTo.
  ///
  /// In en, this message translates to:
  /// **'Pay to {phone}'**
  String payTo(String phone);

  /// No description provided for @full.
  ///
  /// In en, this message translates to:
  /// **'Full'**
  String get full;

  /// No description provided for @postOpen.
  ///
  /// In en, this message translates to:
  /// **'Open'**
  String get postOpen;

  /// No description provided for @postCalledOff.
  ///
  /// In en, this message translates to:
  /// **'Called off'**
  String get postCalledOff;

  /// No description provided for @postOver.
  ///
  /// In en, this message translates to:
  /// **'Over'**
  String get postOver;

  /// No description provided for @playersWanted.
  ///
  /// In en, this message translates to:
  /// **'Players wanted'**
  String get playersWanted;

  /// No description provided for @opponentWanted.
  ///
  /// In en, this message translates to:
  /// **'Opponent wanted'**
  String get opponentWanted;

  /// No description provided for @placeToBeAgreed.
  ///
  /// In en, this message translates to:
  /// **'Place to be agreed'**
  String get placeToBeAgreed;

  /// No description provided for @games_.
  ///
  /// In en, this message translates to:
  /// **'Games'**
  String get games_;

  /// No description provided for @gamesIntro.
  ///
  /// In en, this message translates to:
  /// **'Short of players, or looking for a team to play? Post it, or join one.'**
  String get gamesIntro;

  /// No description provided for @postAGame.
  ///
  /// In en, this message translates to:
  /// **'Post a game'**
  String get postAGame;

  /// No description provided for @openGames.
  ///
  /// In en, this message translates to:
  /// **'Open games'**
  String get openGames;

  /// No description provided for @yours.
  ///
  /// In en, this message translates to:
  /// **'Yours'**
  String get yours;

  /// No description provided for @all.
  ///
  /// In en, this message translates to:
  /// **'All'**
  String get all;

  /// No description provided for @anyDay.
  ///
  /// In en, this message translates to:
  /// **'Any day'**
  String get anyDay;

  /// No description provided for @noPostedGames.
  ///
  /// In en, this message translates to:
  /// **'You haven\'t posted or joined a game'**
  String get noPostedGames;

  /// No description provided for @noOpenGames.
  ///
  /// In en, this message translates to:
  /// **'No open games'**
  String get noOpenGames;

  /// No description provided for @noGamesBody.
  ///
  /// In en, this message translates to:
  /// **'Post one, and players nearby can ask to join.'**
  String get noGamesBody;

  /// No description provided for @yourPost.
  ///
  /// In en, this message translates to:
  /// **'Your post'**
  String get yourPost;

  /// No description provided for @youAskedToJoin.
  ///
  /// In en, this message translates to:
  /// **'You asked to join'**
  String get youAskedToJoin;

  /// No description provided for @youreIn.
  ///
  /// In en, this message translates to:
  /// **'You\'re in'**
  String get youreIn;

  /// No description provided for @notThisTime.
  ///
  /// In en, this message translates to:
  /// **'Not this time'**
  String get notThisTime;

  /// No description provided for @youWithdrew.
  ///
  /// In en, this message translates to:
  /// **'You withdrew'**
  String get youWithdrew;

  /// No description provided for @game.
  ///
  /// In en, this message translates to:
  /// **'Game'**
  String get game;

  /// No description provided for @noLongerTakingPlayers.
  ///
  /// In en, this message translates to:
  /// **'This game is no longer taking players.'**
  String get noLongerTakingPlayers;

  /// No description provided for @askingToJoin.
  ///
  /// In en, this message translates to:
  /// **'Asking to join'**
  String get askingToJoin;

  /// No description provided for @noOneYet.
  ///
  /// In en, this message translates to:
  /// **'No one yet. You\'re told when someone asks.'**
  String get noOneYet;

  /// No description provided for @forTeamBefore.
  ///
  /// In en, this message translates to:
  /// **'For '**
  String get forTeamBefore;

  /// No description provided for @forTeamAfter.
  ///
  /// In en, this message translates to:
  /// **''**
  String get forTeamAfter;

  /// No description provided for @accept.
  ///
  /// In en, this message translates to:
  /// **'Accept'**
  String get accept;

  /// No description provided for @decline.
  ///
  /// In en, this message translates to:
  /// **'Decline'**
  String get decline;

  /// No description provided for @accepted.
  ///
  /// In en, this message translates to:
  /// **'Accepted'**
  String get accepted;

  /// No description provided for @declined.
  ///
  /// In en, this message translates to:
  /// **'Declined'**
  String get declined;

  /// No description provided for @withdrew.
  ///
  /// In en, this message translates to:
  /// **'Withdrew'**
  String get withdrew;

  /// No description provided for @callGameOff.
  ///
  /// In en, this message translates to:
  /// **'Call the game off'**
  String get callGameOff;

  /// No description provided for @youreInExclaim.
  ///
  /// In en, this message translates to:
  /// **'You\'re in!'**
  String get youreInExclaim;

  /// No description provided for @withdraw.
  ///
  /// In en, this message translates to:
  /// **'Withdraw'**
  String get withdraw;

  /// No description provided for @playThemWithTeam.
  ///
  /// In en, this message translates to:
  /// **'Play them with your team'**
  String get playThemWithTeam;

  /// No description provided for @askToJoin.
  ///
  /// In en, this message translates to:
  /// **'Ask to join'**
  String get askToJoin;

  /// No description provided for @needCaptainToTakeOn.
  ///
  /// In en, this message translates to:
  /// **'You need to captain a team to take this on.'**
  String get needCaptainToTakeOn;

  /// No description provided for @yourTeam.
  ///
  /// In en, this message translates to:
  /// **'Your team'**
  String get yourTeam;

  /// No description provided for @joinMessageHint.
  ///
  /// In en, this message translates to:
  /// **'e.g. Can play in goal'**
  String get joinMessageHint;

  /// No description provided for @askingEllipsis.
  ///
  /// In en, this message translates to:
  /// **'Asking…'**
  String get askingEllipsis;

  /// No description provided for @theySeeProfile.
  ///
  /// In en, this message translates to:
  /// **'They see your player profile.'**
  String get theySeeProfile;

  /// No description provided for @editIt.
  ///
  /// In en, this message translates to:
  /// **'Edit it'**
  String get editIt;

  /// No description provided for @sayWhereWhen.
  ///
  /// In en, this message translates to:
  /// **'Say where (a venue or a city) and when, or pick one of your bookings.'**
  String get sayWhereWhen;

  /// No description provided for @whatDoYouNeed.
  ///
  /// In en, this message translates to:
  /// **'What do you need?'**
  String get whatDoYouNeed;

  /// No description provided for @needPlayersOption.
  ///
  /// In en, this message translates to:
  /// **'Players'**
  String get needPlayersOption;

  /// No description provided for @needPlayersDetail.
  ///
  /// In en, this message translates to:
  /// **'We\'re short for a game'**
  String get needPlayersDetail;

  /// No description provided for @needOpponentOption.
  ///
  /// In en, this message translates to:
  /// **'An opponent'**
  String get needOpponentOption;

  /// No description provided for @needOpponentDetail.
  ///
  /// In en, this message translates to:
  /// **'Our team wants a match'**
  String get needOpponentDetail;

  /// No description provided for @needCaptainForOpponent.
  ///
  /// In en, this message translates to:
  /// **'You need to captain a team to find an opponent.'**
  String get needCaptainForOpponent;

  /// No description provided for @whereAndWhen.
  ///
  /// In en, this message translates to:
  /// **'Where and when'**
  String get whereAndWhen;

  /// No description provided for @notBookedYet.
  ///
  /// In en, this message translates to:
  /// **'Not booked yet'**
  String get notBookedYet;

  /// No description provided for @notDecidedCity.
  ///
  /// In en, this message translates to:
  /// **'Not decided: just a city'**
  String get notDecidedCity;

  /// No description provided for @city.
  ///
  /// In en, this message translates to:
  /// **'City'**
  String get city;

  /// No description provided for @cityHint.
  ///
  /// In en, this message translates to:
  /// **'Kathmandu'**
  String get cityHint;

  /// No description provided for @when.
  ///
  /// In en, this message translates to:
  /// **'When'**
  String get when;

  /// No description provided for @pickTime.
  ///
  /// In en, this message translates to:
  /// **'Pick a time'**
  String get pickTime;

  /// No description provided for @playersNeeded.
  ///
  /// In en, this message translates to:
  /// **'Players needed'**
  String get playersNeeded;

  /// No description provided for @eachPays.
  ///
  /// In en, this message translates to:
  /// **'Each pays'**
  String get eachPays;

  /// No description provided for @level.
  ///
  /// In en, this message translates to:
  /// **'Level'**
  String get level;

  /// No description provided for @note.
  ///
  /// In en, this message translates to:
  /// **'Note'**
  String get note;

  /// No description provided for @postNoteHint.
  ///
  /// In en, this message translates to:
  /// **'e.g. Friendly game, need a keeper'**
  String get postNoteHint;

  /// No description provided for @postIt.
  ///
  /// In en, this message translates to:
  /// **'Post it'**
  String get postIt;

  /// No description provided for @posting.
  ///
  /// In en, this message translates to:
  /// **'Posting…'**
  String get posting;

  /// No description provided for @teamWantsGame.
  ///
  /// In en, this message translates to:
  /// **'{team} want a game'**
  String teamWantsGame(String team);

  /// No description provided for @playersNeededCount.
  ///
  /// In en, this message translates to:
  /// **'{count, plural, =1{1 player needed} other{{count} players needed}}'**
  String playersNeededCount(int count);

  /// No description provided for @yourPostAsking.
  ///
  /// In en, this message translates to:
  /// **'Your post · {count} asking to join'**
  String yourPostAsking(int count);

  /// No description provided for @seeVenue.
  ///
  /// In en, this message translates to:
  /// **'See {venue}'**
  String seeVenue(String venue);

  /// No description provided for @callOrMessage.
  ///
  /// In en, this message translates to:
  /// **'Call or message {name} on {phone}.'**
  String callOrMessage(String name, String phone);

  /// No description provided for @pickedSomeoneElse.
  ///
  /// In en, this message translates to:
  /// **'{name} picked someone else this time.'**
  String pickedSomeoneElse(String name);

  /// No description provided for @askedToJoinDecides.
  ///
  /// In en, this message translates to:
  /// **'You asked to join. {name} decides, and you\'re told.'**
  String askedToJoinDecides(String name);

  /// No description provided for @giveTeamName.
  ///
  /// In en, this message translates to:
  /// **'Give the team a name.'**
  String get giveTeamName;

  /// No description provided for @teams.
  ///
  /// In en, this message translates to:
  /// **'Teams'**
  String get teams;

  /// No description provided for @invitations.
  ///
  /// In en, this message translates to:
  /// **'Invitations'**
  String get invitations;

  /// No description provided for @join.
  ///
  /// In en, this message translates to:
  /// **'Join'**
  String get join;

  /// No description provided for @yourTeams.
  ///
  /// In en, this message translates to:
  /// **'Your teams'**
  String get yourTeams;

  /// No description provided for @newTeam.
  ///
  /// In en, this message translates to:
  /// **'New team'**
  String get newTeam;

  /// No description provided for @createTeam.
  ///
  /// In en, this message translates to:
  /// **'Create team'**
  String get createTeam;

  /// No description provided for @creating.
  ///
  /// In en, this message translates to:
  /// **'Creating…'**
  String get creating;

  /// No description provided for @youllBeCaptain.
  ///
  /// In en, this message translates to:
  /// **'You\'ll be the captain, and can invite players by phone.'**
  String get youllBeCaptain;

  /// No description provided for @notInTeam.
  ///
  /// In en, this message translates to:
  /// **'You\'re not in a team yet. Start one, or ask a captain to invite you.'**
  String get notInTeam;

  /// No description provided for @findATeam.
  ///
  /// In en, this message translates to:
  /// **'Find a team'**
  String get findATeam;

  /// No description provided for @teamName.
  ///
  /// In en, this message translates to:
  /// **'Team name'**
  String get teamName;

  /// No description provided for @search.
  ///
  /// In en, this message translates to:
  /// **'Search'**
  String get search;

  /// No description provided for @noPublicTeams.
  ///
  /// In en, this message translates to:
  /// **'No public teams found.'**
  String get noPublicTeams;

  /// No description provided for @enterTheirMobile.
  ///
  /// In en, this message translates to:
  /// **'Enter their 10-digit mobile number.'**
  String get enterTheirMobile;

  /// No description provided for @team.
  ///
  /// In en, this message translates to:
  /// **'Team'**
  String get team;

  /// No description provided for @squad.
  ///
  /// In en, this message translates to:
  /// **'Squad'**
  String get squad;

  /// No description provided for @invited.
  ///
  /// In en, this message translates to:
  /// **'Invited'**
  String get invited;

  /// No description provided for @inviteAPlayer.
  ///
  /// In en, this message translates to:
  /// **'Invite a player'**
  String get inviteAPlayer;

  /// No description provided for @inviteHelp.
  ///
  /// In en, this message translates to:
  /// **'By the phone number they use FutSlot with. They\'re told and can join.'**
  String get inviteHelp;

  /// No description provided for @invite.
  ///
  /// In en, this message translates to:
  /// **'Invite'**
  String get invite;

  /// No description provided for @leaveNeedsNewCaptain.
  ///
  /// In en, this message translates to:
  /// **'To leave, make someone else captain first.'**
  String get leaveNeedsNewCaptain;

  /// No description provided for @cancelInvite.
  ///
  /// In en, this message translates to:
  /// **'Cancel invite'**
  String get cancelInvite;

  /// No description provided for @profileIntro.
  ///
  /// In en, this message translates to:
  /// **'Shown to captains and players when you ask to join a game or a team.'**
  String get profileIntro;

  /// No description provided for @position.
  ///
  /// In en, this message translates to:
  /// **'Position'**
  String get position;

  /// No description provided for @strongerFoot.
  ///
  /// In en, this message translates to:
  /// **'Stronger foot'**
  String get strongerFoot;

  /// No description provided for @aboutYou.
  ///
  /// In en, this message translates to:
  /// **'About you'**
  String get aboutYou;

  /// No description provided for @aboutYouHint.
  ///
  /// In en, this message translates to:
  /// **'e.g. Quick on the wing, play most Friday evenings'**
  String get aboutYouHint;

  /// No description provided for @profilePublic.
  ///
  /// In en, this message translates to:
  /// **'Let other players see my profile'**
  String get profilePublic;

  /// No description provided for @invitedToJoin.
  ///
  /// In en, this message translates to:
  /// **'You\'re invited to join {team}.'**
  String invitedToJoin(String team);

  /// No description provided for @leaveTeam.
  ///
  /// In en, this message translates to:
  /// **'Leave {team}'**
  String leaveTeam(String team);

  /// No description provided for @changeMember.
  ///
  /// In en, this message translates to:
  /// **'Change {name}'**
  String changeMember(String name);

  /// No description provided for @makeRole.
  ///
  /// In en, this message translates to:
  /// **'Make {role}'**
  String makeRole(String role);

  /// No description provided for @gamesPlayedOnFutSlot.
  ///
  /// In en, this message translates to:
  /// **'{games} played on FutSlot'**
  String gamesPlayedOnFutSlot(String games);

  /// No description provided for @tournaments.
  ///
  /// In en, this message translates to:
  /// **'Tournaments'**
  String get tournaments;

  /// No description provided for @comingUp.
  ///
  /// In en, this message translates to:
  /// **'Coming up'**
  String get comingUp;

  /// No description provided for @finished.
  ///
  /// In en, this message translates to:
  /// **'Finished'**
  String get finished;

  /// No description provided for @yourTeamsTab.
  ///
  /// In en, this message translates to:
  /// **'Your teams'**
  String get yourTeamsTab;

  /// No description provided for @noTournamentsComingUp.
  ///
  /// In en, this message translates to:
  /// **'No tournaments coming up'**
  String get noTournamentsComingUp;

  /// No description provided for @noFinishedTournaments.
  ///
  /// In en, this message translates to:
  /// **'No finished tournaments yet'**
  String get noFinishedTournaments;

  /// No description provided for @checkBackSoon.
  ///
  /// In en, this message translates to:
  /// **'Check back soon.'**
  String get checkBackSoon;

  /// No description provided for @finishedTournamentsShow.
  ///
  /// In en, this message translates to:
  /// **'Finished tournaments and their winners show here.'**
  String get finishedTournamentsShow;

  /// No description provided for @teamsNotEntered.
  ///
  /// In en, this message translates to:
  /// **'Your teams haven\'t entered a tournament yet'**
  String get teamsNotEntered;

  /// No description provided for @teamsNotEnteredBody.
  ///
  /// In en, this message translates to:
  /// **'A team\'s captain or vice-captain enters it from the tournament\'s page.'**
  String get teamsNotEnteredBody;

  /// No description provided for @freeToEnter.
  ///
  /// In en, this message translates to:
  /// **'Free to enter'**
  String get freeToEnter;

  /// No description provided for @vs.
  ///
  /// In en, this message translates to:
  /// **'vs'**
  String get vs;

  /// No description provided for @venueCouldntFindPayment.
  ///
  /// In en, this message translates to:
  /// **'The venue couldn\'t find this payment.'**
  String get venueCouldntFindPayment;

  /// No description provided for @tournament.
  ///
  /// In en, this message translates to:
  /// **'Tournament'**
  String get tournament;

  /// No description provided for @format.
  ///
  /// In en, this message translates to:
  /// **'Format'**
  String get format;

  /// No description provided for @entry.
  ///
  /// In en, this message translates to:
  /// **'Entry'**
  String get entry;

  /// No description provided for @teamsFact.
  ///
  /// In en, this message translates to:
  /// **'Teams'**
  String get teamsFact;

  /// No description provided for @squads.
  ///
  /// In en, this message translates to:
  /// **'Squads'**
  String get squads;

  /// No description provided for @prizes.
  ///
  /// In en, this message translates to:
  /// **'Prizes'**
  String get prizes;

  /// No description provided for @rules.
  ///
  /// In en, this message translates to:
  /// **'Rules'**
  String get rules;

  /// No description provided for @table.
  ///
  /// In en, this message translates to:
  /// **'Table'**
  String get table;

  /// No description provided for @fixtures.
  ///
  /// In en, this message translates to:
  /// **'Fixtures'**
  String get fixtures;

  /// No description provided for @teamsIn.
  ///
  /// In en, this message translates to:
  /// **'Teams in'**
  String get teamsIn;

  /// No description provided for @venueChecksPlace.
  ///
  /// In en, this message translates to:
  /// **'The venue checks it and confirms your place.'**
  String get venueChecksPlace;

  /// No description provided for @enterATeam.
  ///
  /// In en, this message translates to:
  /// **'Enter a team'**
  String get enterATeam;

  /// No description provided for @enterYourTeam.
  ///
  /// In en, this message translates to:
  /// **'Enter your team'**
  String get enterYourTeam;

  /// No description provided for @onlyCaptainCanEnter.
  ///
  /// In en, this message translates to:
  /// **'Only a team\'s captain or vice-captain can enter it.'**
  String get onlyCaptainCanEnter;

  /// No description provided for @onlyActiveMembers.
  ///
  /// In en, this message translates to:
  /// **'Only active team members can play. Invite more from the team\'s page.'**
  String get onlyActiveMembers;

  /// No description provided for @enter.
  ///
  /// In en, this message translates to:
  /// **'Enter'**
  String get enter;

  /// No description provided for @entering.
  ///
  /// In en, this message translates to:
  /// **'Entering…'**
  String get entering;

  /// No description provided for @shirtNo.
  ///
  /// In en, this message translates to:
  /// **'No.'**
  String get shirtNo;

  /// No description provided for @colTeam.
  ///
  /// In en, this message translates to:
  /// **'Team'**
  String get colTeam;

  /// No description provided for @colPlayed.
  ///
  /// In en, this message translates to:
  /// **'P'**
  String get colPlayed;

  /// No description provided for @colWon.
  ///
  /// In en, this message translates to:
  /// **'W'**
  String get colWon;

  /// No description provided for @colDrawn.
  ///
  /// In en, this message translates to:
  /// **'D'**
  String get colDrawn;

  /// No description provided for @colLost.
  ///
  /// In en, this message translates to:
  /// **'L'**
  String get colLost;

  /// No description provided for @colGoalDiff.
  ///
  /// In en, this message translates to:
  /// **'GD'**
  String get colGoalDiff;

  /// No description provided for @colPoints.
  ///
  /// In en, this message translates to:
  /// **'Pts'**
  String get colPoints;

  /// No description provided for @toBeDecided.
  ///
  /// In en, this message translates to:
  /// **'To be decided'**
  String get toBeDecided;

  /// No description provided for @walkover.
  ///
  /// In en, this message translates to:
  /// **'Walkover'**
  String get walkover;

  /// No description provided for @penalties.
  ///
  /// In en, this message translates to:
  /// **' ({home}–{away} pens)'**
  String penalties(String home, String away);

  /// No description provided for @perTeam.
  ///
  /// In en, this message translates to:
  /// **'{amount} a team'**
  String perTeam(String amount);

  /// No description provided for @teamsOfMax.
  ///
  /// In en, this message translates to:
  /// **'{count}/{max} teams'**
  String teamsOfMax(String count, String max);

  /// No description provided for @groupName.
  ///
  /// In en, this message translates to:
  /// **'Group {group}'**
  String groupName(String group);

  /// No description provided for @championWon.
  ///
  /// In en, this message translates to:
  /// **'{team} won'**
  String championWon(String team);

  /// No description provided for @teamsOf.
  ///
  /// In en, this message translates to:
  /// **'{count} of {max}'**
  String teamsOf(String count, String max);

  /// No description provided for @squadSize.
  ///
  /// In en, this message translates to:
  /// **'{min}–{max} players'**
  String squadSize(String min, String max);

  /// No description provided for @venueCheckingPlace.
  ///
  /// In en, this message translates to:
  /// **'{venue} is checking it, and we\'ll let you know when your place is confirmed.'**
  String venueCheckingPlace(String venue);

  /// No description provided for @lastPaymentRejected.
  ///
  /// In en, this message translates to:
  /// **'Your last payment wasn\'t accepted: {reason}'**
  String lastPaymentRejected(String reason);

  /// No description provided for @payAmountTo.
  ///
  /// In en, this message translates to:
  /// **'Pay {amount} to {venue}'**
  String payAmountTo(String amount, String venue);

  /// No description provided for @entriesClose.
  ///
  /// In en, this message translates to:
  /// **'Entries close {date}.'**
  String entriesClose(String date);

  /// No description provided for @squadCount.
  ///
  /// In en, this message translates to:
  /// **'Squad ({count} of {min}–{max})'**
  String squadCount(String count, String min, String max);

  /// No description provided for @enterThenPay.
  ///
  /// In en, this message translates to:
  /// **'Enter, then pay {amount}'**
  String enterThenPay(String amount);

  /// No description provided for @shirtNumberFor.
  ///
  /// In en, this message translates to:
  /// **'Shirt number for {name}'**
  String shirtNumberFor(String name);

  /// No description provided for @pickAtLeastPlayers.
  ///
  /// In en, this message translates to:
  /// **'Pick at least {count} players.'**
  String pickAtLeastPlayers(int count);

  /// No description provided for @amenityParking.
  ///
  /// In en, this message translates to:
  /// **'Parking'**
  String get amenityParking;

  /// No description provided for @amenityChangingRoom.
  ///
  /// In en, this message translates to:
  /// **'Changing room'**
  String get amenityChangingRoom;

  /// No description provided for @amenityShower.
  ///
  /// In en, this message translates to:
  /// **'Shower'**
  String get amenityShower;

  /// No description provided for @amenityDrinkingWater.
  ///
  /// In en, this message translates to:
  /// **'Drinking water'**
  String get amenityDrinkingWater;

  /// No description provided for @amenityCafe.
  ///
  /// In en, this message translates to:
  /// **'Cafe / canteen'**
  String get amenityCafe;

  /// No description provided for @amenityFloodlights.
  ///
  /// In en, this message translates to:
  /// **'Floodlights'**
  String get amenityFloodlights;

  /// No description provided for @amenityWifi.
  ///
  /// In en, this message translates to:
  /// **'Wi-Fi'**
  String get amenityWifi;

  /// No description provided for @amenityFirstAid.
  ///
  /// In en, this message translates to:
  /// **'First aid'**
  String get amenityFirstAid;

  /// No description provided for @amenityEquipmentRental.
  ///
  /// In en, this message translates to:
  /// **'Equipment rental'**
  String get amenityEquipmentRental;

  /// No description provided for @amenitySpectatorSeating.
  ///
  /// In en, this message translates to:
  /// **'Spectator seating'**
  String get amenitySpectatorSeating;
}

class _SDelegate extends LocalizationsDelegate<S> {
  const _SDelegate();

  @override
  Future<S> load(Locale locale) {
    return SynchronousFuture<S>(lookupS(locale));
  }

  @override
  bool isSupported(Locale locale) =>
      <String>['en', 'ne'].contains(locale.languageCode);

  @override
  bool shouldReload(_SDelegate old) => false;
}

S lookupS(Locale locale) {
  // Lookup logic when only language code is specified.
  switch (locale.languageCode) {
    case 'en':
      return SEn();
    case 'ne':
      return SNe();
  }

  throw FlutterError(
    'S.delegate failed to load unsupported locale "$locale". This is likely '
    'an issue with the localizations generation tool. Please file an issue '
    'on GitHub with a reproducible sample app and the gen-l10n configuration '
    'that was used.',
  );
}
