// ignore: unused_import
import 'package:intl/intl.dart' as intl;
import 'strings.dart';

// ignore_for_file: type=lint

/// The translations for English (`en`).
class SEn extends S {
  SEn([String locale = 'en']) : super(locale);

  @override
  String get justNow => 'just now';

  @override
  String minutesAgo(String minutes) {
    return '$minutes min ago';
  }

  @override
  String hoursAgo(String hours) {
    return '$hours h ago';
  }

  @override
  String get methodCash => 'Cash';

  @override
  String get methodBankTransfer => 'Bank transfer';

  @override
  String players(int count) {
    final intl.NumberFormat countNumberFormat =
        intl.NumberFormat.decimalPattern(localeName);
    final String countString = countNumberFormat.format(count);

    String _temp0 = intl.Intl.pluralLogic(
      count,
      locale: localeName,
      other: '$countString players',
      one: '1 player',
    );
    return '$_temp0';
  }

  @override
  String games(int count) {
    final intl.NumberFormat countNumberFormat =
        intl.NumberFormat.decimalPattern(localeName);
    final String countString = countNumberFormat.format(count);

    String _temp0 = intl.Intl.pluralLogic(
      count,
      locale: localeName,
      other: '$countString games',
      one: '1 game',
    );
    return '$_temp0';
  }

  @override
  String hours(int count) {
    final intl.NumberFormat countNumberFormat =
        intl.NumberFormat.decimalPattern(localeName);
    final String countString = countNumberFormat.format(count);

    String _temp0 = intl.Intl.pluralLogic(
      count,
      locale: localeName,
      other: '$countString hours',
      one: '1 hour',
    );
    return '$_temp0';
  }

  @override
  String days(int count) {
    final intl.NumberFormat countNumberFormat =
        intl.NumberFormat.decimalPattern(localeName);
    final String countString = countNumberFormat.format(count);

    String _temp0 = intl.Intl.pluralLogic(
      count,
      locale: localeName,
      other: '$countString days',
      one: '1 day',
    );
    return '$_temp0';
  }

  @override
  String listAnd(String first, String last) {
    return '$first and $last';
  }

  @override
  String get skillBeginner => 'Beginner';

  @override
  String get skillIntermediate => 'Intermediate';

  @override
  String get skillAdvanced => 'Advanced';

  @override
  String get skillPro => 'Pro';

  @override
  String get positionGoalkeeper => 'Goalkeeper';

  @override
  String get positionDefender => 'Defender';

  @override
  String get positionMidfielder => 'Midfielder';

  @override
  String get positionForward => 'Forward';

  @override
  String get positionAny => 'Anywhere';

  @override
  String get footLeft => 'Left';

  @override
  String get footRight => 'Right';

  @override
  String get footBoth => 'Both';

  @override
  String get roleCaptain => 'Captain';

  @override
  String get roleViceCaptain => 'Vice-captain';

  @override
  String get roleMember => 'Player';

  @override
  String get formatKnockout => 'Knockout';

  @override
  String get formatLeague => 'League';

  @override
  String get formatGroupKnockout => 'Groups, then knockout';

  @override
  String get tournamentDraft => 'Draft';

  @override
  String get tournamentOpen => 'Taking teams';

  @override
  String get tournamentClosed => 'Entries closed';

  @override
  String get tournamentInProgress => 'Under way';

  @override
  String get tournamentCompleted => 'Finished';

  @override
  String get tournamentCancelled => 'Cancelled';

  @override
  String get stageGroup => 'Group stage';

  @override
  String get stageLeague => 'League';

  @override
  String get stageRoundOf32 => 'Round of 32';

  @override
  String get stageRoundOf16 => 'Round of 16';

  @override
  String get stageQuarterFinal => 'Quarter-finals';

  @override
  String get stageSemiFinal => 'Semi-finals';

  @override
  String get stageThirdPlace => 'Third place';

  @override
  String get stageFinal => 'Final';

  @override
  String get entryPendingPayment => 'Pay the entry fee';

  @override
  String get entryConfirmed => 'Confirmed';

  @override
  String get entryWaitlisted => 'On the waitlist';

  @override
  String get entryWithdrawn => 'Withdrawn';

  @override
  String get entryRejected => 'Not accepted';

  @override
  String get ruleEqual => 'Split equally';

  @override
  String get ruleLosersPayAll => 'Losers pay all';

  @override
  String get ruleLosersPayPercent => 'Losers pay more';

  @override
  String get ruleLosersPayCourt => 'Losers pay the court';

  @override
  String get ruleEqualDetail => 'Everyone pays the same, whoever wins.';

  @override
  String get ruleLosersPayAllDetail =>
      'Baazi: the losing side pays everything.';

  @override
  String get ruleLosersPayPercentDetail =>
      'Baazi: the losing side pays a share you choose, the winners the rest.';

  @override
  String get ruleLosersPayCourtDetail =>
      'Baazi: losers pay for the court, winners for the extras.';

  @override
  String get resultDraw => 'Draw';

  @override
  String sideWon(String side) {
    return '$side won';
  }

  @override
  String planDiscount(String percent) {
    return '$percent% off every game';
  }

  @override
  String planFor(String terms, String days) {
    return '$terms, for $days';
  }

  @override
  String get sunday => 'Sunday';

  @override
  String get monday => 'Monday';

  @override
  String get tuesday => 'Tuesday';

  @override
  String get wednesday => 'Wednesday';

  @override
  String get thursday => 'Thursday';

  @override
  String get friday => 'Friday';

  @override
  String get saturday => 'Saturday';

  @override
  String get closed => 'Closed';

  @override
  String get policyPayAtVenue => 'Book now and pay at the venue.';

  @override
  String get policyFreeCancelAnyTime =>
      'Cancel any time before the game for free.';

  @override
  String get policyFeeDeposit =>
      'What you paid covers it first; your FutSlot deposit covers the rest.';

  @override
  String get policyFeeCapped => 'It\'s never more than you\'ve paid.';

  @override
  String get policyVenueCancels =>
      'If the venue cancels, you get everything back.';

  @override
  String get chipConfirmed => 'Confirmed';

  @override
  String get chipHeld => 'Waiting for payment';

  @override
  String get chipVerify => 'Payment being checked';

  @override
  String get chipCompleted => 'Played';

  @override
  String get chipNoShow => 'No-show';

  @override
  String get chipCancelled => 'Cancelled';

  @override
  String get chipExpired => 'Not paid in time';

  @override
  String policyAdvance(String percent, int minutes) {
    final intl.NumberFormat minutesNumberFormat =
        intl.NumberFormat.decimalPattern(localeName);
    final String minutesString = minutesNumberFormat.format(minutes);

    return 'Pay $percent% up front to confirm. We hold the slot for $minutesString minutes while you pay.';
  }

  @override
  String feeWithin(String percent, String hours) {
    return '$percent% within $hours';
  }

  @override
  String policyFreeBefore(String hours) {
    return 'Cancel $hours or more before the game for free.';
  }

  @override
  String policyLateFee(String fees, String cover) {
    return 'Cancelling later costs a share of the booking price: $fees. $cover';
  }

  @override
  String policyDeposit(String amount) {
    return 'You need a $amount FutSlot booking deposit to book here.';
  }

  @override
  String policyNoShow(String percent) {
    return 'Not showing up costs $percent% of the booking price.';
  }

  @override
  String policyStrikes(int count, String days, String percent) {
    final intl.NumberFormat countNumberFormat =
        intl.NumberFormat.decimalPattern(localeName);
    final String countString = countNumberFormat.format(count);

    String _temp0 = intl.Intl.pluralLogic(
      count,
      locale: localeName,
      other:
          'After $countString late cancellations or no-shows here in $days days, you pay $percent% up front.',
      one:
          'After a late cancellation or no-show here in $days days, you pay $percent% up front.',
    );
    return '$_temp0';
  }

  @override
  String get sendForChecking => 'Send for checking';

  @override
  String get enterTransactionId =>
      'Enter the transaction ID from your receipt.';

  @override
  String get paidWith => 'Paid with';

  @override
  String get amountYouSent => 'Amount you sent';

  @override
  String get transactionId => 'Transaction ID';

  @override
  String get fromYourReceipt => 'From your payment receipt';

  @override
  String get sending => 'Sending…';

  @override
  String get loading => 'Loading';

  @override
  String get tryAgain => 'Try again';

  @override
  String get optional => ' (optional)';

  @override
  String get ratingPoor => 'Poor';

  @override
  String get ratingNotGreat => 'Not great';

  @override
  String get ratingOkay => 'Okay';

  @override
  String get ratingGood => 'Good';

  @override
  String get ratingGreat => 'Great';

  @override
  String get postReview => 'Post review';

  @override
  String get pickStars => 'Pick how many stars.';

  @override
  String get whatWasItLike => 'What was it like?';

  @override
  String get reviewHint => 'The turf, the lights, the staff…';

  @override
  String get saving => 'Saving…';

  @override
  String get notifications => 'Notifications';

  @override
  String get offline =>
      'Can\'t reach FutSlot. Check your connection and try again.';

  @override
  String get somethingWentWrong => 'Something went wrong. Please try again.';

  @override
  String get tabExplore => 'Explore';

  @override
  String get tabBookings => 'Bookings';

  @override
  String get tabPlay => 'Play';

  @override
  String get tabMessages => 'Messages';

  @override
  String get tabMe => 'Me';

  @override
  String get playGames => 'Games';

  @override
  String get playTournaments => 'Tournaments';

  @override
  String get playTeams => 'Teams';

  @override
  String get fromPrice => 'From ';

  @override
  String enterAmountBetween(String min, String max) {
    return 'Enter between $min and $max.';
  }

  @override
  String sendAtLeastTo(String amount) {
    return 'Send at least $amount to';
  }

  @override
  String sendAmountTo(String amount) {
    return 'Send $amount to';
  }

  @override
  String starsOutOfFive(String stars) {
    return '$stars out of 5';
  }

  @override
  String starsOutOfFiveWord(String stars, String word) {
    return '$stars out of 5: $word';
  }

  @override
  String notificationsUnread(int count) {
    final intl.NumberFormat countNumberFormat =
        intl.NumberFormat.decimalPattern(localeName);
    final String countString = countNumberFormat.format(count);

    return 'Notifications, $countString unread';
  }

  @override
  String messagesUnread(int count) {
    final intl.NumberFormat countNumberFormat =
        intl.NumberFormat.decimalPattern(localeName);
    final String countString = countNumberFormat.format(count);

    return 'Messages, $countString unread';
  }

  @override
  String courts(int count) {
    final intl.NumberFormat countNumberFormat =
        intl.NumberFormat.decimalPattern(localeName);
    final String countString = countNumberFormat.format(count);

    String _temp0 = intl.Intl.pluralLogic(
      count,
      locale: localeName,
      other: '$countString courts',
      one: '1 court',
    );
    return '$_temp0';
  }

  @override
  String ratingFrom(String rating, String reviews) {
    return '$rating out of 5 from $reviews';
  }

  @override
  String reviews(int count) {
    final intl.NumberFormat countNumberFormat =
        intl.NumberFormat.decimalPattern(localeName);
    final String countString = countNumberFormat.format(count);

    String _temp0 = intl.Intl.pluralLogic(
      count,
      locale: localeName,
      other: '$countString reviews',
      one: '1 review',
    );
    return '$_temp0';
  }

  @override
  String footSide(String side) {
    return '$side foot';
  }

  @override
  String get enterMobile =>
      'Enter your 10-digit mobile number, like 98XXXXXXXX.';

  @override
  String get enterPassword => 'Enter your password.';

  @override
  String get passwordNotSet =>
      'Your account doesn\'t have a password yet. We\'ll text you a code to set one.';

  @override
  String get codeSentMoment =>
      'We sent you a code a moment ago. Enter it below.';

  @override
  String codeShown(String code) {
    return 'Texts cost money and the developer is broke, so here\'s your code instead: $code. Real SMS comes once a few futsals sign up.';
  }

  @override
  String get accountExists =>
      'This number already has an account. Sign in, or reset your password if you forgot it.';

  @override
  String get noAccountYet =>
      'No account uses this number yet. Create one below.';

  @override
  String get enterCode => 'Enter the 6-digit code we sent.';

  @override
  String get enterName => 'Enter your name.';

  @override
  String get googleFailed =>
      'Google sign-in didn\'t work. Use your number and password instead.';

  @override
  String get signInTitle => 'Sign in to FutSlot';

  @override
  String get signInSubtitle => 'Use your mobile number and password.';

  @override
  String get signUpTitle => 'Create your account';

  @override
  String get signUpSubtitle =>
      'We\'ll text a code to check the number is yours.';

  @override
  String get resetTitle => 'Reset your password';

  @override
  String get resetSubtitle =>
      'We\'ll text a code to your number, then you choose a new password.';

  @override
  String get googleTitle => 'One more step';

  @override
  String get googleSubtitle =>
      'Add your mobile number. Venues and teammates find you by it, so we\'ll text a code to check it\'s yours.';

  @override
  String get backToVenues => 'Back to venues';

  @override
  String get signedOutAgain => 'You were signed out. Please sign in again.';

  @override
  String get backToSignIn => 'Back to sign in';

  @override
  String get hidePassword => 'Hide password';

  @override
  String get showPassword => 'Show password';

  @override
  String get mobileNumber => 'Mobile number';

  @override
  String get password => 'Password';

  @override
  String get newPassword => 'New password';

  @override
  String get forgotPassword => 'Forgot password?';

  @override
  String get signIn => 'Sign in';

  @override
  String get signingIn => 'Signing in…';

  @override
  String get or => 'or';

  @override
  String get continueWithGoogle => 'Continue with Google';

  @override
  String get newToFutSlot => 'New to FutSlot?';

  @override
  String get createAnAccount => 'Create an account';

  @override
  String get sendCode => 'Send code';

  @override
  String get sixDigitCode => '6-digit code';

  @override
  String get change => 'Change';

  @override
  String get yourName => 'Your name';

  @override
  String get venuesSeeName => 'Venues see this name on your bookings.';

  @override
  String get createAccount => 'Create account';

  @override
  String get savePasswordSignIn => 'Save password and sign in';

  @override
  String get continueAction => 'Continue';

  @override
  String get checking => 'Checking…';

  @override
  String get resendCode => 'Resend code';

  @override
  String passwordTooShort(int count) {
    final intl.NumberFormat countNumberFormat =
        intl.NumberFormat.decimalPattern(localeName);
    final String countString = countNumberFormat.format(count);

    return 'Use at least $countString characters for your password.';
  }

  @override
  String passwordHelp(int count) {
    final intl.NumberFormat countNumberFormat =
        intl.NumberFormat.decimalPattern(localeName);
    final String countString = countNumberFormat.format(count);

    return 'At least $countString characters. Not your phone number, or something as easy to guess.';
  }

  @override
  String sentTo(String phone) {
    return 'Sent to $phone · ';
  }

  @override
  String resendIn(int seconds) {
    final intl.NumberFormat secondsNumberFormat =
        intl.NumberFormat.decimalPattern(localeName);
    final String secondsString = secondsNumberFormat.format(seconds);

    return 'Resend code in ${secondsString}s';
  }

  @override
  String get meDeposit => 'Booking deposit';

  @override
  String get meDepositDetail => 'Top up or take it back';

  @override
  String get meProfile => 'Player profile';

  @override
  String get meProfileDetail => 'Your position and level, for games and teams';

  @override
  String get meMemberships => 'Memberships';

  @override
  String get meMembershipsDetail => 'Game packs and passes';

  @override
  String get meDues => 'What you owe';

  @override
  String get meDuesDetail => 'Your share of games friends booked';

  @override
  String get meFavourites => 'Saved venues';

  @override
  String get meFavouritesDetail => 'Venues you like, to book again';

  @override
  String get meReviews => 'Reviews';

  @override
  String get meReviewsDetail => 'Rate the venues you played at';

  @override
  String get meWaitlist => 'Waitlist';

  @override
  String get meWaitlistDetail => 'Booked slots you want if they free up';

  @override
  String get mePassword => 'Password';

  @override
  String get mePasswordDetail => 'Change it, and sign out other devices';

  @override
  String get saved => 'Saved.';

  @override
  String get yourDetails => 'Your details';

  @override
  String get name => 'Name';

  @override
  String get venuesSeeThis => 'Venues see this on your bookings.';

  @override
  String get email => 'Email';

  @override
  String get save => 'Save';

  @override
  String get appearance => 'Appearance';

  @override
  String get themeLight => 'Light';

  @override
  String get themeDark => 'Dark';

  @override
  String get themeAuto => 'Auto';

  @override
  String get signOut => 'Sign out';

  @override
  String get language => 'Language';

  @override
  String get languageHelp => 'Notifications and texts come in it too.';

  @override
  String get useAtLeast8 => 'Use at least 8 characters.';

  @override
  String get enterCurrentPassword => 'Enter your current password.';

  @override
  String get passwordSaved => 'Password saved. Other devices are signed out.';

  @override
  String get hidePasswords => 'Hide passwords';

  @override
  String get showPasswords => 'Show passwords';

  @override
  String get setAPassword => 'Set a password';

  @override
  String get googleOnlyNote =>
      'You sign in with Google. A password lets you sign in with your number too.';

  @override
  String get currentPassword => 'Current password';

  @override
  String get passwordStayHere =>
      'At least 8 characters. You stay signed in here; other devices are signed out.';

  @override
  String get changePassword => 'Change password';

  @override
  String get setPassword => 'Set password';

  @override
  String get exploreTitle => 'Book a futsal court';

  @override
  String get exploreSubtitle =>
      'Free slots and prices at venues near you. Pay the advance with eSewa or Khalti.';

  @override
  String get venueSearchHint => 'Venue name or area';

  @override
  String get clear => 'Clear';

  @override
  String get allCities => 'All cities';

  @override
  String get featured => 'Featured';

  @override
  String get allVenues => 'All venues';

  @override
  String get noVenuesFound => 'No venues found';

  @override
  String get noVenuesFoundBody => 'Try another name or area, or all cities.';

  @override
  String get loadingMore => 'Loading…';

  @override
  String get showMoreVenues => 'Show more venues';

  @override
  String get waitBody =>
      'Wait for it, and we\'ll tell you if it frees up. Whoever books it first then gets it.';

  @override
  String get waitForIt => 'Wait for it';

  @override
  String get saved_ => 'Saved';

  @override
  String get saveVenue => 'Save';

  @override
  String get message => 'Message';

  @override
  String get bookASlot => 'Book a slot';

  @override
  String get bookingRules => 'Booking rules';

  @override
  String get amenities => 'Amenities';

  @override
  String get reviewsTitle => 'Reviews';

  @override
  String get noReviewsYet =>
      'No reviews yet. Players can review after they play here.';

  @override
  String get noCourtsYet => 'No courts are open for booking yet.';

  @override
  String get closedThisDay => 'Closed this day.';

  @override
  String get noMoreGamesToday => 'No more games today.';

  @override
  String get today => 'Today';

  @override
  String get tomorrow => 'Tomorrow';

  @override
  String get fullyBooked => 'Fully booked';

  @override
  String get waiting => 'Waiting';

  @override
  String get booked => 'Booked';

  @override
  String get openingHours => 'Opening hours';

  @override
  String get memberships => 'Memberships';

  @override
  String get membershipsPitch =>
      'Play more, pay less. Use it when you book here.';

  @override
  String get free => 'Free';

  @override
  String get buy => 'Buy';

  @override
  String get iPaid => 'I\'ve paid';

  @override
  String get getIt => 'Get it';

  @override
  String get gettingIt => 'Getting it…';

  @override
  String get notNow => 'Not now';

  @override
  String venuesFound(int count) {
    final intl.NumberFormat countNumberFormat =
        intl.NumberFormat.decimalPattern(localeName);
    final String countString = countNumberFormat.format(count);

    String _temp0 = intl.Intl.pluralLogic(
      count,
      locale: localeName,
      other: '$countString venues',
      one: '1 venue',
    );
    return '$_temp0';
  }

  @override
  String slotBookedOn(String time, String court) {
    return '$time on $court is booked';
  }

  @override
  String weWillTellYou(String time) {
    return 'We\'ll tell you if $time frees up.';
  }

  @override
  String freeSlots(int count) {
    final intl.NumberFormat countNumberFormat =
        intl.NumberFormat.decimalPattern(localeName);
    final String countString = countNumberFormat.format(count);

    return '$countString free';
  }

  @override
  String slotWaiting(String time) {
    return '$time, booked. You\'re waiting for it';
  }

  @override
  String slotWaitQuestion(String time) {
    return '$time is booked. Wait for it?';
  }

  @override
  String slotClosed(String time) {
    return '$time, closed';
  }

  @override
  String slotBook(String time, String price) {
    return '$time, $price. Book';
  }

  @override
  String playedOn(String date) {
    return 'Played $date';
  }

  @override
  String venueReplied(String venue) {
    return '$venue replied';
  }

  @override
  String membershipChecks(String venue) {
    return '$venue checks your payment, then your membership starts.';
  }

  @override
  String get bookThisSlot => 'Book this slot';

  @override
  String get slotUnavailable => 'This slot can\'t be booked';

  @override
  String get seeOtherSlots => 'See other free slots';

  @override
  String get addDepositFirst => 'Add your booking deposit first';

  @override
  String get thisVenue => 'This venue';

  @override
  String get extras => 'Extras';

  @override
  String get extrasPaidAtVenue => 'Paid at the venue with your booking.';

  @override
  String get useMembership => 'Use a membership';

  @override
  String get dontUseOne => 'Don\'t use one';

  @override
  String get discountCode => 'Discount code';

  @override
  String get remove => 'Remove';

  @override
  String get codeHint => 'e.g. DASHAIN20';

  @override
  String get apply => 'Apply';

  @override
  String get noteForVenue => 'Note for the venue';

  @override
  String get noteHint => 'e.g. We\'re 10 players, need bibs';

  @override
  String get workingOutPrice => 'Working out the price';

  @override
  String get court => 'Court';

  @override
  String get discount => 'Discount';

  @override
  String get total => 'Total';

  @override
  String get payNowToConfirm => 'Pay now to confirm';

  @override
  String get atTheVenue => 'At the venue';

  @override
  String get pay => 'Pay';

  @override
  String get strikeAdvanceNote =>
      'You pay more up front here because of your recent late cancellations or no-shows at this venue.';

  @override
  String get book => 'Book';

  @override
  String get booking_ => 'Booking…';

  @override
  String depositExplain(String venue, String amount) {
    return '$venue asks players to keep $amount with FutSlot. It isn\'t spent on your games: it only pays the venue\'s fee if you cancel late or don\'t show up, and you can take it back when you have no games booked.';
  }

  @override
  String addAmount(String amount) {
    return 'Add $amount';
  }

  @override
  String gamesLeft(int count) {
    final intl.NumberFormat countNumberFormat =
        intl.NumberFormat.decimalPattern(localeName);
    final String countString = countNumberFormat.format(count);

    return '$countString left';
  }

  @override
  String bookAndPay(String amount) {
    return 'Book and pay $amount';
  }

  @override
  String holdWhilePay(int minutes) {
    final intl.NumberFormat minutesNumberFormat =
        intl.NumberFormat.decimalPattern(localeName);
    final String minutesString = minutesNumberFormat.format(minutes);

    return 'We hold the slot for $minutesString minutes while you pay the venue.';
  }

  @override
  String priceEach(String price) {
    return '$price each';
  }

  @override
  String oneLess(String item) {
    return 'One less $item';
  }

  @override
  String oneMore(String item) {
    return 'One more $item';
  }

  @override
  String get yourBookings => 'Your bookings';

  @override
  String get upcoming => 'Upcoming';

  @override
  String get past => 'Past';

  @override
  String get noGamesComingUp => 'No games coming up';

  @override
  String get nothingHereYet => 'Nothing here yet';

  @override
  String get findCourtPitch => 'Find a free court and book it in a minute.';

  @override
  String get pastGamesShowHere => 'Games you play or cancel show up here.';

  @override
  String get showMore => 'Show more';

  @override
  String get booking => 'Booking';

  @override
  String get bookedSeeYou => 'Booked. See you on the court!';

  @override
  String get bookedPayBelow => 'Booked. Pay the advance below to confirm it.';

  @override
  String get messageTheVenue => 'Message the venue';

  @override
  String get splitTheCost => 'Split the cost';

  @override
  String get splitTheCostDetail => 'Share it equally, or baazi: the losers pay';

  @override
  String get yourNote => 'Your note';

  @override
  String get cancelled => 'Cancelled';

  @override
  String get cancelledRefund => 'Anything you paid comes back to you.';

  @override
  String get yourReview => 'Your review';

  @override
  String get edit => 'Edit';

  @override
  String get delete => 'Delete';

  @override
  String get editYourReview => 'Edit your review';

  @override
  String get reviewShownAs =>
      'Other players see it with your first name and last initial.';

  @override
  String get cancel => 'Cancel';

  @override
  String get paymentSent => 'Payment sent';

  @override
  String get payDirectly =>
      'Pay the venue directly, then tell us the transaction ID. The venue checks it and confirms your booking.';

  @override
  String get timeToPayRanOut => 'The time to pay has run out';

  @override
  String get slotMayBeGone =>
      'The slot may have gone to someone else. Book again if it\'s still free.';

  @override
  String get seeFreeSlots => 'See free slots';

  @override
  String get payment => 'Payment';

  @override
  String get paid => 'Paid';

  @override
  String get beingChecked => ' · being checked';

  @override
  String get started => ' · started';

  @override
  String get notReceived => ' · not received';

  @override
  String get paymentCancelled => ' · cancelled';

  @override
  String get refunded => ' · refunded';

  @override
  String get partlyRefunded => ' · partly refunded';

  @override
  String get cancelBooking => 'Cancel booking';

  @override
  String get cancelThisBooking => 'Cancel this booking?';

  @override
  String get why => 'Why?';

  @override
  String get cancelling => 'Cancelling…';

  @override
  String get keepIt => 'Keep it';

  @override
  String get freeToCancel => 'It\'s free to cancel now.';

  @override
  String bookingRef(String reference) {
    return 'Booking $reference';
  }

  @override
  String timeRange(String start, String end) {
    return '$start – $end';
  }

  @override
  String bookingSummaryLabel(
    String day,
    String start,
    String end,
    String court,
  ) {
    return '$day, $start to $end, $court';
  }

  @override
  String cancelledLate(String percent) {
    return 'Cancelled late, so the venue kept its $percent% fee.';
  }

  @override
  String cancelledLateDeposit(String percent) {
    return 'Cancelled late, so the venue kept its $percent% fee, with any part your advance didn\'t cover taken from your deposit.';
  }

  @override
  String howWasVenue(String venue) {
    return 'How was $venue?';
  }

  @override
  String venueChecking(String venue) {
    return '$venue is checking it. Your slot is held meanwhile, and we\'ll let you know when it\'s confirmed.';
  }

  @override
  String payToConfirm(String amount) {
    return 'Pay $amount to confirm';
  }

  @override
  String heldFor(int count, String time) {
    final intl.NumberFormat countNumberFormat =
        intl.NumberFormat.decimalPattern(localeName);
    final String countString = countNumberFormat.format(count);

    String _temp0 = intl.Intl.pluralLogic(
      count,
      locale: localeName,
      other: 'Held for $countString more minutes, until $time.',
      one: 'Held for 1 more minute, until $time.',
    );
    return '$_temp0';
  }

  @override
  String lineItem(int quantity, String name) {
    final intl.NumberFormat quantityNumberFormat =
        intl.NumberFormat.decimalPattern(localeName);
    final String quantityString = quantityNumberFormat.format(quantity);

    return '$quantityString × $name';
  }

  @override
  String refundBy(String method, String state) {
    return 'Refund by $method · $state';
  }

  @override
  String get refundSent => 'sent';

  @override
  String get refundOnItsWay => 'on its way';

  @override
  String youGetBack(String amount) {
    return 'You get $amount back.';
  }

  @override
  String cancelCosts(String percent, String fee) {
    return 'Cancelling now costs $percent% of the price ($fee).';
  }

  @override
  String venueKeeps(String amount) {
    return 'The venue keeps $amount of what you paid.';
  }

  @override
  String venueKeepsRefund(String amount, String refund) {
    return 'The venue keeps $amount of what you paid and you get $refund back.';
  }

  @override
  String fromDeposit(String amount) {
    return 'Up to $amount more comes out of your FutSlot deposit.';
  }

  @override
  String get bankAccount => 'Bank account';

  @override
  String get enterSendTo => 'Enter the number or account to send it to.';

  @override
  String get toppedUp => 'Topped up';

  @override
  String get takenBack => 'Taken back';

  @override
  String get keptAfterAll => 'Kept after all';

  @override
  String get bookingDeposit => 'Booking deposit';

  @override
  String get inYourDeposit => 'In your deposit';

  @override
  String get deposit => 'Deposit';

  @override
  String get depositFull => 'Full: you can book any venue.';

  @override
  String get backToBooking => 'Back to your booking';

  @override
  String get keepDeposit => 'Keep my deposit instead';

  @override
  String get topUpSent => 'Top-up sent';

  @override
  String get topUpChecking =>
      'FutSlot is checking it, usually within a few hours. You can book once it\'s in.';

  @override
  String get payFutSlotDirectly =>
      'Pay FutSlot directly, then tell us the transaction ID.';

  @override
  String get history => 'History';

  @override
  String get takeDepositBack => 'Take your deposit back';

  @override
  String get takeItBack => 'Take it back';

  @override
  String get sendItToMy => 'Send it to my';

  @override
  String get bankNameAccount => 'Bank name and account number';

  @override
  String get number => 'Number';

  @override
  String get asking => 'Asking…';

  @override
  String get askForItBack => 'Ask for it back';

  @override
  String get back => 'Back';

  @override
  String get membershipsIntro =>
      'Game packs and passes you bought at venues. They\'re used when you book there.';

  @override
  String get noMembershipsYet => 'No memberships yet';

  @override
  String get noMembershipsBody =>
      'Venues that sell game packs or passes list them on their page.';

  @override
  String get active => 'Active';

  @override
  String get expired => 'Expired';

  @override
  String get savedVenues => 'Saved venues';

  @override
  String get noSavedVenues => 'No saved venues';

  @override
  String get noSavedVenuesBody =>
      'Tap the heart on a venue you like to find it here.';

  @override
  String get howWasYourGame => 'How was your game?';

  @override
  String get reviewWindow =>
      'You can review a game for 30 days after playing it.';

  @override
  String get review => 'Review';

  @override
  String get yourReviews => 'Your reviews';

  @override
  String get noReviewsWritten => 'You haven\'t reviewed a venue yet.';

  @override
  String get reviewHidden => 'Hidden by FutSlot while it checks it';

  @override
  String get reviewRemoved => 'Removed by FutSlot';

  @override
  String get waitlist => 'Waitlist';

  @override
  String get waitlistIntro =>
      'Slots you want that someone else booked. If one frees up we tell you, and whoever books it first gets it.';

  @override
  String get notWaiting => 'You\'re not waiting for any slot';

  @override
  String get notWaitingBody =>
      'On a venue\'s page, tap a booked slot to wait for it.';

  @override
  String get itFreedUp => 'It freed up. Book it before someone else does.';

  @override
  String get stopWaiting => 'Stop waiting';

  @override
  String get markAllRead => 'Mark all read';

  @override
  String get nothingYet => 'Nothing yet';

  @override
  String get notificationsEmpty =>
      'Booking confirmations, reminders and replies from venues show up here.';

  @override
  String get showOlder => 'Show older';

  @override
  String get unread => 'Unread';

  @override
  String feeToVenue(String venue, String reference) {
    return 'Fee to $venue · $reference';
  }

  @override
  String depositIntro(String amount) {
    return 'Some venues only take bookings from players who keep $amount with FutSlot. It isn\'t spent on your games: it only pays a venue\'s fee if you cancel late or don\'t show up. You can take it back whenever you have no games booked there.';
  }

  @override
  String ofAmount(String amount) {
    return ' of $amount';
  }

  @override
  String comingBack(String amount) {
    return '$amount is coming back to you';
  }

  @override
  String withdrawalWhen(String method, String sendTo, String date) {
    return 'FutSlot sends it to your $method $sendTo on $date. Until then you can\'t book venues that need the deposit.';
  }

  @override
  String get depositInUse => 'Your deposit is covering a game';

  @override
  String get depositInUseWhen =>
      'You can take it back once the games you\'ve booked at venues that need it are played or cancelled.';

  @override
  String withdrawExplain(String amount) {
    return 'FutSlot sends all of $amount to you 3 days after you ask.';
  }

  @override
  String until(String date) {
    return 'Until $date';
  }

  @override
  String ended(String date) {
    return 'Ended $date';
  }

  @override
  String get messages => 'Messages';

  @override
  String get findAVenue => 'Find a venue';

  @override
  String get noMessagesYet => 'No messages yet';

  @override
  String get noMessagesBody =>
      'Ask a venue about courts, a booking or anything else: tap Message on its page or on your booking.';

  @override
  String get typeMessageFirst => 'Type a message first.';

  @override
  String get venue => 'Venue';

  @override
  String get showEarlier => 'Show earlier messages';

  @override
  String get send => 'Send';

  @override
  String get seen => ' · Seen';

  @override
  String get yesterday => 'Yesterday';

  @override
  String nobodyMatches(String text) {
    return 'Nobody matches \"$text\".';
  }

  @override
  String unreadCount(int count) {
    final intl.NumberFormat countNumberFormat =
        intl.NumberFormat.decimalPattern(localeName);
    final String countString = countNumberFormat.format(count);

    return '$countString unread';
  }

  @override
  String askVenueBooking(String venue) {
    return 'Ask $venue about courts, prices or this booking. They reply here, and we\'ll let you know.';
  }

  @override
  String askVenue(String venue) {
    return 'Ask $venue about courts, prices or a booking. They reply here, and we\'ll let you know.';
  }

  @override
  String messageTo(String venue) {
    return 'Message $venue';
  }

  @override
  String get giveNameOrPhone =>
      'Give their name, or the phone number they use FutSlot with.';

  @override
  String get changeSplit => 'Change how the cost is shared.';

  @override
  String get startSplitIntro =>
      'Share what this game costs with the players: equally, or baazi-style.';

  @override
  String get whoPays => 'Who pays';

  @override
  String get losingSidePays => 'The losing side pays';

  @override
  String get percentOfEverything => '% of everything';

  @override
  String get oneSide => 'One side';

  @override
  String get otherSide => 'The other side';

  @override
  String get teamA => 'Team A';

  @override
  String get teamB => 'Team B';

  @override
  String get youPlayFor => 'You play for';

  @override
  String get extrasRs => 'Extras (Rs)';

  @override
  String get forWhat => 'For';

  @override
  String get extrasHint => 'Water, drinks…';

  @override
  String get startSplitting => 'Start splitting';

  @override
  String get played => 'Played';

  @override
  String get undo => 'Undo';

  @override
  String get playedTheGame => 'Played the game?';

  @override
  String get whoWon => 'Who won?';

  @override
  String get worksOutShares =>
      'This works out everyone\'s share and tells the players with the app.';

  @override
  String get workOutShares => 'Work out shares';

  @override
  String get addAPlayer => 'Add a player';

  @override
  String get addPlayerHelp =>
      'By the phone number they use FutSlot with, so they see their share, or just a name.';

  @override
  String get phone => 'Phone';

  @override
  String get add => 'Add';

  @override
  String get eachPaysRoughly => 'Each player pays, roughly';

  @override
  String get ifResult => 'If';

  @override
  String get you => ' (you)';

  @override
  String get bookedBy => ' (booked)';

  @override
  String get whatYouOwe => 'What you owe';

  @override
  String get duesIntro =>
      'Your share of games other players booked. Pay them, and they tick you off.';

  @override
  String get allSquare => 'You\'re all square';

  @override
  String get allSquareBody =>
      'When someone adds you to a game they booked and splits the cost, your share shows up here.';

  @override
  String get inAll => ' in all';

  @override
  String extrasFor(String note) {
    return 'Extras · $note';
  }

  @override
  String collectedSoFar(String collected, String outstanding) {
    return '$collected collected · $outstanding still to come';
  }

  @override
  String shareOnceResult(String name) {
    return 'Your share is worked out once $name records the result.';
  }

  @override
  String sharePaid(String name, String amount) {
    return 'Paid. $name has your $amount.';
  }

  @override
  String youOwe(String name, String amount) {
    return 'You owe $name $amount';
  }

  @override
  String payThemOn(String phone) {
    return 'Pay them on eSewa or Khalti: $phone. They tick you off once it\'s in.';
  }

  @override
  String paidName(String name) {
    return 'Paid: $name';
  }

  @override
  String markPaidName(String name) {
    return 'Mark paid: $name';
  }

  @override
  String moveToOtherSide(String name) {
    return 'Move $name to the other side';
  }

  @override
  String removeName(String name) {
    return 'Remove $name';
  }

  @override
  String payTo(String phone) {
    return 'Pay to $phone';
  }

  @override
  String get full => 'Full';

  @override
  String get postOpen => 'Open';

  @override
  String get postCalledOff => 'Called off';

  @override
  String get postOver => 'Over';

  @override
  String get playersWanted => 'Players wanted';

  @override
  String get opponentWanted => 'Opponent wanted';

  @override
  String get placeToBeAgreed => 'Place to be agreed';

  @override
  String get games_ => 'Games';

  @override
  String get gamesIntro =>
      'Short of players, or looking for a team to play? Post it, or join one.';

  @override
  String get postAGame => 'Post a game';

  @override
  String get openGames => 'Open games';

  @override
  String get yours => 'Yours';

  @override
  String get all => 'All';

  @override
  String get anyDay => 'Any day';

  @override
  String get noPostedGames => 'You haven\'t posted or joined a game';

  @override
  String get noOpenGames => 'No open games';

  @override
  String get noGamesBody => 'Post one, and players nearby can ask to join.';

  @override
  String get yourPost => 'Your post';

  @override
  String get youAskedToJoin => 'You asked to join';

  @override
  String get youreIn => 'You\'re in';

  @override
  String get notThisTime => 'Not this time';

  @override
  String get youWithdrew => 'You withdrew';

  @override
  String get game => 'Game';

  @override
  String get noLongerTakingPlayers => 'This game is no longer taking players.';

  @override
  String get askingToJoin => 'Asking to join';

  @override
  String get noOneYet => 'No one yet. You\'re told when someone asks.';

  @override
  String get forTeamBefore => 'For ';

  @override
  String get forTeamAfter => '';

  @override
  String get accept => 'Accept';

  @override
  String get decline => 'Decline';

  @override
  String get accepted => 'Accepted';

  @override
  String get declined => 'Declined';

  @override
  String get withdrew => 'Withdrew';

  @override
  String get callGameOff => 'Call the game off';

  @override
  String get youreInExclaim => 'You\'re in!';

  @override
  String get withdraw => 'Withdraw';

  @override
  String get playThemWithTeam => 'Play them with your team';

  @override
  String get askToJoin => 'Ask to join';

  @override
  String get needCaptainToTakeOn =>
      'You need to captain a team to take this on.';

  @override
  String get yourTeam => 'Your team';

  @override
  String get joinMessageHint => 'e.g. Can play in goal';

  @override
  String get askingEllipsis => 'Asking…';

  @override
  String get theySeeProfile => 'They see your player profile.';

  @override
  String get editIt => 'Edit it';

  @override
  String get sayWhereWhen =>
      'Say where (a venue or a city) and when, or pick one of your bookings.';

  @override
  String get whatDoYouNeed => 'What do you need?';

  @override
  String get needPlayersOption => 'Players';

  @override
  String get needPlayersDetail => 'We\'re short for a game';

  @override
  String get needOpponentOption => 'An opponent';

  @override
  String get needOpponentDetail => 'Our team wants a match';

  @override
  String get needCaptainForOpponent =>
      'You need to captain a team to find an opponent.';

  @override
  String get whereAndWhen => 'Where and when';

  @override
  String get notBookedYet => 'Not booked yet';

  @override
  String get notDecidedCity => 'Not decided: just a city';

  @override
  String get city => 'City';

  @override
  String get cityHint => 'Kathmandu';

  @override
  String get when => 'When';

  @override
  String get pickTime => 'Pick a time';

  @override
  String get playersNeeded => 'Players needed';

  @override
  String get eachPays => 'Each pays';

  @override
  String get level => 'Level';

  @override
  String get note => 'Note';

  @override
  String get postNoteHint => 'e.g. Friendly game, need a keeper';

  @override
  String get postIt => 'Post it';

  @override
  String get posting => 'Posting…';

  @override
  String teamWantsGame(String team) {
    return '$team want a game';
  }

  @override
  String playersNeededCount(int count) {
    final intl.NumberFormat countNumberFormat =
        intl.NumberFormat.decimalPattern(localeName);
    final String countString = countNumberFormat.format(count);

    String _temp0 = intl.Intl.pluralLogic(
      count,
      locale: localeName,
      other: '$countString players needed',
      one: '1 player needed',
    );
    return '$_temp0';
  }

  @override
  String yourPostAsking(int count) {
    final intl.NumberFormat countNumberFormat =
        intl.NumberFormat.decimalPattern(localeName);
    final String countString = countNumberFormat.format(count);

    return 'Your post · $countString asking to join';
  }

  @override
  String seeVenue(String venue) {
    return 'See $venue';
  }

  @override
  String callOrMessage(String name, String phone) {
    return 'Call or message $name on $phone.';
  }

  @override
  String pickedSomeoneElse(String name) {
    return '$name picked someone else this time.';
  }

  @override
  String askedToJoinDecides(String name) {
    return 'You asked to join. $name decides, and you\'re told.';
  }

  @override
  String get giveTeamName => 'Give the team a name.';

  @override
  String get teams => 'Teams';

  @override
  String get invitations => 'Invitations';

  @override
  String get join => 'Join';

  @override
  String get yourTeams => 'Your teams';

  @override
  String get newTeam => 'New team';

  @override
  String get createTeam => 'Create team';

  @override
  String get creating => 'Creating…';

  @override
  String get youllBeCaptain =>
      'You\'ll be the captain, and can invite players by phone.';

  @override
  String get notInTeam =>
      'You\'re not in a team yet. Start one, or ask a captain to invite you.';

  @override
  String get findATeam => 'Find a team';

  @override
  String get teamName => 'Team name';

  @override
  String get search => 'Search';

  @override
  String get noPublicTeams => 'No public teams found.';

  @override
  String get enterTheirMobile => 'Enter their 10-digit mobile number.';

  @override
  String get team => 'Team';

  @override
  String get squad => 'Squad';

  @override
  String get invited => 'Invited';

  @override
  String get inviteAPlayer => 'Invite a player';

  @override
  String get inviteHelp =>
      'By the phone number they use FutSlot with. They\'re told and can join.';

  @override
  String get invite => 'Invite';

  @override
  String get leaveNeedsNewCaptain =>
      'To leave, make someone else captain first.';

  @override
  String get cancelInvite => 'Cancel invite';

  @override
  String get profileIntro =>
      'Shown to captains and players when you ask to join a game or a team.';

  @override
  String get position => 'Position';

  @override
  String get strongerFoot => 'Stronger foot';

  @override
  String get aboutYou => 'About you';

  @override
  String get aboutYouHint =>
      'e.g. Quick on the wing, play most Friday evenings';

  @override
  String get profilePublic => 'Let other players see my profile';

  @override
  String invitedToJoin(String team) {
    return 'You\'re invited to join $team.';
  }

  @override
  String leaveTeam(String team) {
    return 'Leave $team';
  }

  @override
  String changeMember(String name) {
    return 'Change $name';
  }

  @override
  String makeRole(String role) {
    return 'Make $role';
  }

  @override
  String gamesPlayedOnFutSlot(String games) {
    return '$games played on FutSlot';
  }

  @override
  String get tournaments => 'Tournaments';

  @override
  String get comingUp => 'Coming up';

  @override
  String get finished => 'Finished';

  @override
  String get yourTeamsTab => 'Your teams';

  @override
  String get noTournamentsComingUp => 'No tournaments coming up';

  @override
  String get noFinishedTournaments => 'No finished tournaments yet';

  @override
  String get checkBackSoon => 'Check back soon.';

  @override
  String get finishedTournamentsShow =>
      'Finished tournaments and their winners show here.';

  @override
  String get teamsNotEntered => 'Your teams haven\'t entered a tournament yet';

  @override
  String get teamsNotEnteredBody =>
      'A team\'s captain or vice-captain enters it from the tournament\'s page.';

  @override
  String get freeToEnter => 'Free to enter';

  @override
  String get vs => 'vs';

  @override
  String get venueCouldntFindPayment =>
      'The venue couldn\'t find this payment.';

  @override
  String get tournament => 'Tournament';

  @override
  String get format => 'Format';

  @override
  String get entry => 'Entry';

  @override
  String get teamsFact => 'Teams';

  @override
  String get squads => 'Squads';

  @override
  String get prizes => 'Prizes';

  @override
  String get rules => 'Rules';

  @override
  String get table => 'Table';

  @override
  String get fixtures => 'Fixtures';

  @override
  String get teamsIn => 'Teams in';

  @override
  String get venueChecksPlace => 'The venue checks it and confirms your place.';

  @override
  String get enterATeam => 'Enter a team';

  @override
  String get enterYourTeam => 'Enter your team';

  @override
  String get onlyCaptainCanEnter =>
      'Only a team\'s captain or vice-captain can enter it.';

  @override
  String get onlyActiveMembers =>
      'Only active team members can play. Invite more from the team\'s page.';

  @override
  String get enter => 'Enter';

  @override
  String get entering => 'Entering…';

  @override
  String get shirtNo => 'No.';

  @override
  String get colTeam => 'Team';

  @override
  String get colPlayed => 'P';

  @override
  String get colWon => 'W';

  @override
  String get colDrawn => 'D';

  @override
  String get colLost => 'L';

  @override
  String get colGoalDiff => 'GD';

  @override
  String get colPoints => 'Pts';

  @override
  String get toBeDecided => 'To be decided';

  @override
  String get walkover => 'Walkover';

  @override
  String penalties(String home, String away) {
    return ' ($home–$away pens)';
  }

  @override
  String perTeam(String amount) {
    return '$amount a team';
  }

  @override
  String teamsOfMax(String count, String max) {
    return '$count/$max teams';
  }

  @override
  String groupName(String group) {
    return 'Group $group';
  }

  @override
  String championWon(String team) {
    return '$team won';
  }

  @override
  String teamsOf(String count, String max) {
    return '$count of $max';
  }

  @override
  String squadSize(String min, String max) {
    return '$min–$max players';
  }

  @override
  String venueCheckingPlace(String venue) {
    return '$venue is checking it, and we\'ll let you know when your place is confirmed.';
  }

  @override
  String lastPaymentRejected(String reason) {
    return 'Your last payment wasn\'t accepted: $reason';
  }

  @override
  String payAmountTo(String amount, String venue) {
    return 'Pay $amount to $venue';
  }

  @override
  String entriesClose(String date) {
    return 'Entries close $date.';
  }

  @override
  String squadCount(String count, String min, String max) {
    return 'Squad ($count of $min–$max)';
  }

  @override
  String enterThenPay(String amount) {
    return 'Enter, then pay $amount';
  }

  @override
  String shirtNumberFor(String name) {
    return 'Shirt number for $name';
  }

  @override
  String pickAtLeastPlayers(int count) {
    final intl.NumberFormat countNumberFormat =
        intl.NumberFormat.decimalPattern(localeName);
    final String countString = countNumberFormat.format(count);

    return 'Pick at least $countString players.';
  }

  @override
  String get amenityParking => 'Parking';

  @override
  String get amenityChangingRoom => 'Changing room';

  @override
  String get amenityShower => 'Shower';

  @override
  String get amenityDrinkingWater => 'Drinking water';

  @override
  String get amenityCafe => 'Cafe / canteen';

  @override
  String get amenityFloodlights => 'Floodlights';

  @override
  String get amenityWifi => 'Wi-Fi';

  @override
  String get amenityFirstAid => 'First aid';

  @override
  String get amenityEquipmentRental => 'Equipment rental';

  @override
  String get amenitySpectatorSeating => 'Spectator seating';
}
