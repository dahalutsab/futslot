// ignore: unused_import
import 'package:intl/intl.dart' as intl;
import 'strings.dart';

// ignore_for_file: type=lint

/// The translations for Nepali (`ne`).
class SNe extends S {
  SNe([String locale = 'ne']) : super(locale);

  @override
  String get justNow => 'भर्खरै';

  @override
  String minutesAgo(String minutes) {
    return '$minutes मिनेट अघि';
  }

  @override
  String hoursAgo(String hours) {
    return '$hours घण्टा अघि';
  }

  @override
  String get methodCash => 'नगद';

  @override
  String get methodBankTransfer => 'बैंक ट्रान्सफर';

  @override
  String players(int count) {
    final intl.NumberFormat countNumberFormat =
        intl.NumberFormat.decimalPattern(localeName);
    final String countString = countNumberFormat.format(count);

    return '$countString खेलाडी';
  }

  @override
  String games(int count) {
    final intl.NumberFormat countNumberFormat =
        intl.NumberFormat.decimalPattern(localeName);
    final String countString = countNumberFormat.format(count);

    return '$countString खेल';
  }

  @override
  String hours(int count) {
    final intl.NumberFormat countNumberFormat =
        intl.NumberFormat.decimalPattern(localeName);
    final String countString = countNumberFormat.format(count);

    return '$countString घण्टा';
  }

  @override
  String days(int count) {
    final intl.NumberFormat countNumberFormat =
        intl.NumberFormat.decimalPattern(localeName);
    final String countString = countNumberFormat.format(count);

    return '$countString दिन';
  }

  @override
  String listAnd(String first, String last) {
    return '$first र $last';
  }

  @override
  String get skillBeginner => 'सुरुवाती';

  @override
  String get skillIntermediate => 'मध्यम';

  @override
  String get skillAdvanced => 'उन्नत';

  @override
  String get skillPro => 'प्रो';

  @override
  String get positionGoalkeeper => 'गोलकिपर';

  @override
  String get positionDefender => 'डिफेन्डर';

  @override
  String get positionMidfielder => 'मिडफिल्डर';

  @override
  String get positionForward => 'फरवार्ड';

  @override
  String get positionAny => 'जहाँ पनि';

  @override
  String get footLeft => 'देब्रे';

  @override
  String get footRight => 'दाहिने';

  @override
  String get footBoth => 'दुवै';

  @override
  String get roleCaptain => 'कप्तान';

  @override
  String get roleViceCaptain => 'उपकप्तान';

  @override
  String get roleMember => 'खेलाडी';

  @override
  String get formatKnockout => 'नकआउट';

  @override
  String get formatLeague => 'लिग';

  @override
  String get formatGroupKnockout => 'समूह, त्यसपछि नकआउट';

  @override
  String get tournamentDraft => 'ड्राफ्ट';

  @override
  String get tournamentOpen => 'टिम दर्ता हुँदै';

  @override
  String get tournamentClosed => 'दर्ता बन्द';

  @override
  String get tournamentInProgress => 'चलिरहेको';

  @override
  String get tournamentCompleted => 'सकियो';

  @override
  String get tournamentCancelled => 'रद्द';

  @override
  String get stageGroup => 'समूह चरण';

  @override
  String get stageLeague => 'लिग';

  @override
  String get stageRoundOf32 => 'अन्तिम ३२';

  @override
  String get stageRoundOf16 => 'अन्तिम १६';

  @override
  String get stageQuarterFinal => 'क्वार्टरफाइनल';

  @override
  String get stageSemiFinal => 'सेमिफाइनल';

  @override
  String get stageThirdPlace => 'तेस्रो स्थान';

  @override
  String get stageFinal => 'फाइनल';

  @override
  String get entryPendingPayment => 'प्रवेश शुल्क तिर्नुहोस्';

  @override
  String get entryConfirmed => 'पक्का';

  @override
  String get entryWaitlisted => 'पर्खाइ सूचीमा';

  @override
  String get entryWithdrawn => 'फिर्ता लिइयो';

  @override
  String get entryRejected => 'स्वीकार भएन';

  @override
  String get ruleEqual => 'बराबर बाँड्ने';

  @override
  String get ruleLosersPayAll => 'हार्नेले सबै तिर्ने';

  @override
  String get ruleLosersPayPercent => 'हार्नेले बढी तिर्ने';

  @override
  String get ruleLosersPayCourt => 'हार्नेले कोर्ट तिर्ने';

  @override
  String get ruleEqualDetail => 'जसले जिते पनि, सबैले बराबर तिर्ने।';

  @override
  String get ruleLosersPayAllDetail => 'बाजी: हार्ने टिमले सबै तिर्छ।';

  @override
  String get ruleLosersPayPercentDetail =>
      'बाजी: हार्ने टिमले तपाईंले छानेको हिस्सा, जित्नेले बाँकी तिर्छ।';

  @override
  String get ruleLosersPayCourtDetail =>
      'बाजी: हार्नेले कोर्टको, जित्नेले अरू सामान (पानी आदि) को तिर्छ।';

  @override
  String get resultDraw => 'बराबरी';

  @override
  String sideWon(String side) {
    return '$side जित्यो';
  }

  @override
  String planDiscount(String percent) {
    return 'हरेक खेलमा $percent% छुट';
  }

  @override
  String planFor(String terms, String days) {
    return '$terms, $daysको लागि';
  }

  @override
  String get sunday => 'आइतबार';

  @override
  String get monday => 'सोमबार';

  @override
  String get tuesday => 'मंगलबार';

  @override
  String get wednesday => 'बुधबार';

  @override
  String get thursday => 'बिहिबार';

  @override
  String get friday => 'शुक्रबार';

  @override
  String get saturday => 'शनिबार';

  @override
  String get closed => 'बन्द';

  @override
  String get policyPayAtVenue =>
      'अहिले बुक गर्नुहोस्, पैसा भेन्युमै तिर्नुहोस्।';

  @override
  String get policyFreeCancelAnyTime =>
      'खेल सुरु हुनुअघि जुनसुकै बेला निःशुल्क रद्द गर्न सकिन्छ।';

  @override
  String get policyFeeDeposit =>
      'पहिले तपाईंले तिरेको रकमबाट कटिन्छ, बाँकी FutSlot डिपोजिटबाट।';

  @override
  String get policyFeeCapped => 'तपाईंले तिरेको भन्दा बढी कहिल्यै लाग्दैन।';

  @override
  String get policyVenueCancels => 'भेन्युले रद्द गरेमा सबै पैसा फिर्ता हुन्छ।';

  @override
  String get chipConfirmed => 'पक्का';

  @override
  String get chipHeld => 'भुक्तानी पर्खँदै';

  @override
  String get chipVerify => 'भुक्तानी जाँच हुँदै';

  @override
  String get chipCompleted => 'खेलियो';

  @override
  String get chipNoShow => 'आएनन्';

  @override
  String get chipCancelled => 'रद्द';

  @override
  String get chipExpired => 'समयमा तिरिएन';

  @override
  String policyAdvance(String percent, int minutes) {
    final intl.NumberFormat minutesNumberFormat =
        intl.NumberFormat.decimalPattern(localeName);
    final String minutesString = minutesNumberFormat.format(minutes);

    return 'पक्का गर्न $percent% अग्रिम तिर्नुहोस्। तपाईंले तिर्दासम्म स्लट $minutesString मिनेट होल्ड गरिन्छ।';
  }

  @override
  String feeWithin(String percent, String hours) {
    return '$hours भित्र $percent%';
  }

  @override
  String policyFreeBefore(String hours) {
    return 'खेलभन्दा $hours वा त्योभन्दा अघि रद्द गरे निःशुल्क।';
  }

  @override
  String policyLateFee(String fees, String cover) {
    return 'त्यसपछि रद्द गरे बुकिङ मूल्यको केही हिस्सा लाग्छ: $fees। $cover';
  }

  @override
  String policyDeposit(String amount) {
    return 'यहाँ बुक गर्न $amount को FutSlot बुकिङ डिपोजिट चाहिन्छ।';
  }

  @override
  String policyNoShow(String percent) {
    return 'नआएमा बुकिङ मूल्यको $percent% लाग्छ।';
  }

  @override
  String policyStrikes(int count, String days, String percent) {
    final intl.NumberFormat countNumberFormat =
        intl.NumberFormat.decimalPattern(localeName);
    final String countString = countNumberFormat.format(count);

    return 'यहाँ $days दिनभित्र $countString पटक ढिलो रद्द गरे वा नआए, $percent% अग्रिम तिर्नुपर्छ।';
  }

  @override
  String get sendForChecking => 'जाँचका लागि पठाउनुहोस्';

  @override
  String get enterTransactionId =>
      'रसिदमा भएको ट्रान्ज्याक्सन आइडी लेख्नुहोस्।';

  @override
  String get paidWith => 'कसरी तिर्नुभयो';

  @override
  String get amountYouSent => 'तपाईंले पठाएको रकम';

  @override
  String get transactionId => 'ट्रान्ज्याक्सन आइडी';

  @override
  String get fromYourReceipt => 'भुक्तानीको रसिदबाट';

  @override
  String get sending => 'पठाउँदै…';

  @override
  String get loading => 'लोड हुँदै';

  @override
  String get tryAgain => 'फेरि प्रयास गर्नुहोस्';

  @override
  String get optional => ' (ऐच्छिक)';

  @override
  String get ratingPoor => 'नराम्रो';

  @override
  String get ratingNotGreat => 'त्यति राम्रो होइन';

  @override
  String get ratingOkay => 'ठीकै';

  @override
  String get ratingGood => 'राम्रो';

  @override
  String get ratingGreat => 'उत्कृष्ट';

  @override
  String get postReview => 'समीक्षा पोस्ट गर्नुहोस्';

  @override
  String get pickStars => 'कति तारा दिने छान्नुहोस्।';

  @override
  String get whatWasItLike => 'कस्तो लाग्यो?';

  @override
  String get reviewHint => 'टर्फ, बत्ती, कर्मचारी…';

  @override
  String get saving => 'सेभ हुँदै…';

  @override
  String get notifications => 'सूचनाहरू';

  @override
  String get offline =>
      'FutSlot सम्म पुग्न सकिएन। इन्टरनेट जाँचेर फेरि प्रयास गर्नुहोस्।';

  @override
  String get somethingWentWrong => 'केही गडबड भयो। फेरि प्रयास गर्नुहोस्।';

  @override
  String get tabExplore => 'खोज्नुहोस्';

  @override
  String get tabBookings => 'बुकिङ';

  @override
  String get tabPlay => 'खेल';

  @override
  String get tabMessages => 'सन्देश';

  @override
  String get tabMe => 'म';

  @override
  String get playGames => 'खेलहरू';

  @override
  String get playTournaments => 'प्रतियोगिता';

  @override
  String get playTeams => 'टिमहरू';

  @override
  String get fromPrice => 'सुरु मूल्य ';

  @override
  String enterAmountBetween(String min, String max) {
    return '$min देखि $max सम्म लेख्नुहोस्।';
  }

  @override
  String sendAtLeastTo(String amount) {
    return 'कम्तीमा $amount यहाँ पठाउनुहोस्:';
  }

  @override
  String sendAmountTo(String amount) {
    return '$amount यहाँ पठाउनुहोस्:';
  }

  @override
  String starsOutOfFive(String stars) {
    return '५ मा $stars';
  }

  @override
  String starsOutOfFiveWord(String stars, String word) {
    return '५ मा $stars: $word';
  }

  @override
  String notificationsUnread(int count) {
    final intl.NumberFormat countNumberFormat =
        intl.NumberFormat.decimalPattern(localeName);
    final String countString = countNumberFormat.format(count);

    return 'सूचनाहरू, $countString नपढिएको';
  }

  @override
  String messagesUnread(int count) {
    final intl.NumberFormat countNumberFormat =
        intl.NumberFormat.decimalPattern(localeName);
    final String countString = countNumberFormat.format(count);

    return 'सन्देश, $countString नपढिएको';
  }

  @override
  String courts(int count) {
    final intl.NumberFormat countNumberFormat =
        intl.NumberFormat.decimalPattern(localeName);
    final String countString = countNumberFormat.format(count);

    return '$countString कोर्ट';
  }

  @override
  String ratingFrom(String rating, String reviews) {
    return '$reviews मा ५ मध्ये $rating';
  }

  @override
  String reviews(int count) {
    final intl.NumberFormat countNumberFormat =
        intl.NumberFormat.decimalPattern(localeName);
    final String countString = countNumberFormat.format(count);

    return '$countString समीक्षा';
  }

  @override
  String footSide(String side) {
    return '$side खुट्टा';
  }

  @override
  String get enterMobile =>
      '१० अङ्कको मोबाइल नम्बर लेख्नुहोस्, जस्तै 98XXXXXXXX।';

  @override
  String get enterPassword => 'पासवर्ड लेख्नुहोस्।';

  @override
  String get passwordNotSet =>
      'तपाईंको खातामा अझै पासवर्ड छैन। पासवर्ड राख्न हामी कोड पठाउँछौं।';

  @override
  String get codeSentMoment => 'भर्खरै कोड पठाइसकेका छौं। तल लेख्नुहोस्।';

  @override
  String get accountExists =>
      'यो नम्बरमा पहिले नै खाता छ। साइन इन गर्नुहोस्, वा पासवर्ड बिर्सनुभएको भए रिसेट गर्नुहोस्।';

  @override
  String get noAccountYet =>
      'यो नम्बरमा अहिलेसम्म खाता छैन। तल खाता बनाउनुहोस्।';

  @override
  String get enterCode => 'हामीले पठाएको ६ अङ्कको कोड लेख्नुहोस्।';

  @override
  String get enterName => 'आफ्नो नाम लेख्नुहोस्।';

  @override
  String get googleFailed =>
      'Google बाट साइन इन भएन। नम्बर र पासवर्डले साइन इन गर्नुहोस्।';

  @override
  String get signInTitle => 'FutSlot मा साइन इन';

  @override
  String get signInSubtitle => 'मोबाइल नम्बर र पासवर्ड प्रयोग गर्नुहोस्।';

  @override
  String get signUpTitle => 'खाता बनाउनुहोस्';

  @override
  String get signUpSubtitle => 'नम्बर तपाईंकै हो भनी जाँच्न हामी कोड पठाउँछौं।';

  @override
  String get resetTitle => 'पासवर्ड रिसेट गर्नुहोस्';

  @override
  String get resetSubtitle =>
      'तपाईंको नम्बरमा कोड पठाउँछौं, त्यसपछि नयाँ पासवर्ड छान्नुहोस्।';

  @override
  String get googleTitle => 'अब एउटा मात्र काम';

  @override
  String get googleSubtitle =>
      'मोबाइल नम्बर थप्नुहोस्। भेन्यु र साथीहरूले तपाईंलाई यही नम्बरले भेट्छन्, त्यसैले नम्बर तपाईंकै हो भनी जाँच्न कोड पठाउँछौं।';

  @override
  String get backToVenues => 'भेन्युहरूमा फर्कनुहोस्';

  @override
  String get signedOutAgain =>
      'तपाईं साइन आउट हुनुभयो। फेरि साइन इन गर्नुहोस्।';

  @override
  String get backToSignIn => 'साइन इनमा फर्कनुहोस्';

  @override
  String get hidePassword => 'पासवर्ड लुकाउनुहोस्';

  @override
  String get showPassword => 'पासवर्ड देखाउनुहोस्';

  @override
  String get mobileNumber => 'मोबाइल नम्बर';

  @override
  String get password => 'पासवर्ड';

  @override
  String get newPassword => 'नयाँ पासवर्ड';

  @override
  String get forgotPassword => 'पासवर्ड बिर्सनुभयो?';

  @override
  String get signIn => 'साइन इन';

  @override
  String get signingIn => 'साइन इन हुँदै…';

  @override
  String get or => 'वा';

  @override
  String get continueWithGoogle => 'Google बाट जारी राख्नुहोस्';

  @override
  String get newToFutSlot => 'FutSlot मा नयाँ?';

  @override
  String get createAnAccount => 'खाता बनाउनुहोस्';

  @override
  String get sendCode => 'कोड पठाउनुहोस्';

  @override
  String get sixDigitCode => '६ अङ्कको कोड';

  @override
  String get change => 'बदल्नुहोस्';

  @override
  String get yourName => 'तपाईंको नाम';

  @override
  String get venuesSeeName => 'तपाईंको बुकिङमा भेन्युले यही नाम देख्छ।';

  @override
  String get createAccount => 'खाता बनाउनुहोस्';

  @override
  String get savePasswordSignIn => 'पासवर्ड सेभ गरेर साइन इन';

  @override
  String get continueAction => 'जारी राख्नुहोस्';

  @override
  String get checking => 'जाँच हुँदै…';

  @override
  String get resendCode => 'कोड फेरि पठाउनुहोस्';

  @override
  String passwordTooShort(int count) {
    final intl.NumberFormat countNumberFormat =
        intl.NumberFormat.decimalPattern(localeName);
    final String countString = countNumberFormat.format(count);

    return 'पासवर्डमा कम्तीमा $countString अक्षर राख्नुहोस्।';
  }

  @override
  String passwordHelp(int count) {
    final intl.NumberFormat countNumberFormat =
        intl.NumberFormat.decimalPattern(localeName);
    final String countString = countNumberFormat.format(count);

    return 'कम्तीमा $countString अक्षर। फोन नम्बर वा सजिलै अनुमान गर्न सकिने कुरा नराख्नुहोस्।';
  }

  @override
  String sentTo(String phone) {
    return '$phone मा पठाइयो · ';
  }

  @override
  String resendIn(int seconds) {
    final intl.NumberFormat secondsNumberFormat =
        intl.NumberFormat.decimalPattern(localeName);
    final String secondsString = secondsNumberFormat.format(seconds);

    return '$secondsString सेकेन्डमा फेरि पठाउन सकिन्छ';
  }

  @override
  String get meDeposit => 'बुकिङ डिपोजिट';

  @override
  String get meDepositDetail => 'रकम थप्नुहोस् वा फिर्ता लिनुहोस्';

  @override
  String get meProfile => 'खेलाडी प्रोफाइल';

  @override
  String get meProfileDetail => 'खेल र टिमका लागि तपाईंको पोजिसन र स्तर';

  @override
  String get meMemberships => 'सदस्यता';

  @override
  String get meMembershipsDetail => 'खेल प्याक र पासहरू';

  @override
  String get meDues => 'तपाईंले तिर्नुपर्ने';

  @override
  String get meDuesDetail => 'साथीहरूले बुक गरेका खेलमा तपाईंको भाग';

  @override
  String get meFavourites => 'सेभ गरिएका भेन्यु';

  @override
  String get meFavouritesDetail => 'फेरि बुक गर्न मन परेका भेन्यु';

  @override
  String get meReviews => 'समीक्षा';

  @override
  String get meReviewsDetail => 'खेलेका भेन्युलाई मूल्याङ्कन गर्नुहोस्';

  @override
  String get meWaitlist => 'पर्खाइ सूची';

  @override
  String get meWaitlistDetail => 'खाली भए लिन चाहेका बुक भइसकेका स्लट';

  @override
  String get mePassword => 'पासवर्ड';

  @override
  String get mePasswordDetail => 'पासवर्ड बदल्नुहोस्, अरू डिभाइसबाट साइन आउट';

  @override
  String get saved => 'सेभ भयो।';

  @override
  String get yourDetails => 'तपाईंको विवरण';

  @override
  String get name => 'नाम';

  @override
  String get venuesSeeThis => 'तपाईंको बुकिङमा भेन्युले यो देख्छ।';

  @override
  String get email => 'इमेल';

  @override
  String get save => 'सेभ गर्नुहोस्';

  @override
  String get appearance => 'रूप';

  @override
  String get themeLight => 'उज्यालो';

  @override
  String get themeDark => 'अँध्यारो';

  @override
  String get themeAuto => 'स्वतः';

  @override
  String get signOut => 'साइन आउट';

  @override
  String get language => 'भाषा';

  @override
  String get languageHelp => 'सूचना र SMS पनि यही भाषामा आउँछन्।';

  @override
  String get useAtLeast8 => 'कम्तीमा ८ अक्षर राख्नुहोस्।';

  @override
  String get enterCurrentPassword => 'हालको पासवर्ड लेख्नुहोस्।';

  @override
  String get passwordSaved => 'पासवर्ड सेभ भयो। अरू डिभाइसबाट साइन आउट भयो।';

  @override
  String get hidePasswords => 'पासवर्ड लुकाउनुहोस्';

  @override
  String get showPasswords => 'पासवर्ड देखाउनुहोस्';

  @override
  String get setAPassword => 'पासवर्ड राख्नुहोस्';

  @override
  String get googleOnlyNote =>
      'तपाईं Google बाट साइन इन गर्नुहुन्छ। पासवर्ड राखे नम्बरबाट पनि साइन इन गर्न सकिन्छ।';

  @override
  String get currentPassword => 'हालको पासवर्ड';

  @override
  String get passwordStayHere =>
      'कम्तीमा ८ अक्षर। यहाँ साइन इन नै रहनुहुन्छ, अरू डिभाइसबाट साइन आउट हुन्छ।';

  @override
  String get changePassword => 'पासवर्ड बदल्नुहोस्';

  @override
  String get setPassword => 'पासवर्ड राख्नुहोस्';

  @override
  String get exploreTitle => 'फुटसल कोर्ट बुक गर्नुहोस्';

  @override
  String get exploreSubtitle =>
      'नजिकका भेन्युका खाली स्लट र मूल्य। अग्रिम eSewa वा Khalti बाट तिर्नुहोस्।';

  @override
  String get venueSearchHint => 'भेन्युको नाम वा ठाउँ';

  @override
  String get clear => 'हटाउनुहोस्';

  @override
  String get allCities => 'सबै शहर';

  @override
  String get featured => 'विशेष';

  @override
  String get allVenues => 'सबै भेन्यु';

  @override
  String get noVenuesFound => 'कुनै भेन्यु भेटिएन';

  @override
  String get noVenuesFoundBody => 'अर्को नाम वा ठाउँ, वा सबै शहर खोज्नुहोस्।';

  @override
  String get loadingMore => 'लोड हुँदै…';

  @override
  String get showMoreVenues => 'थप भेन्यु देखाउनुहोस्';

  @override
  String get waitBody =>
      'पर्खनुहोस्, खाली भयो भने हामी खबर गर्छौं। त्यसपछि जसले पहिले बुक गर्छ, उसैले पाउँछ।';

  @override
  String get waitForIt => 'पर्खनुहोस्';

  @override
  String get saved_ => 'सेभ गरिएको';

  @override
  String get saveVenue => 'सेभ गर्नुहोस्';

  @override
  String get message => 'सन्देश';

  @override
  String get bookASlot => 'स्लट बुक गर्नुहोस्';

  @override
  String get bookingRules => 'बुकिङका नियम';

  @override
  String get amenities => 'सुविधाहरू';

  @override
  String get reviewsTitle => 'समीक्षाहरू';

  @override
  String get noReviewsYet =>
      'अहिलेसम्म समीक्षा छैन। यहाँ खेलेपछि खेलाडीले समीक्षा लेख्न सक्छन्।';

  @override
  String get noCourtsYet => 'अहिलेसम्म बुकिङका लागि कुनै कोर्ट खुला छैन।';

  @override
  String get closedThisDay => 'यो दिन बन्द।';

  @override
  String get noMoreGamesToday => 'आज अब खेल छैनन्।';

  @override
  String get today => 'आज';

  @override
  String get tomorrow => 'भोलि';

  @override
  String get fullyBooked => 'सबै बुक भइसक्यो';

  @override
  String get waiting => 'पर्खँदै';

  @override
  String get booked => 'बुक भइसक्यो';

  @override
  String get openingHours => 'खुल्ने समय';

  @override
  String get memberships => 'सदस्यता';

  @override
  String get membershipsPitch =>
      'धेरै खेल्नुहोस्, कम तिर्नुहोस्। यहाँ बुक गर्दा प्रयोग गर्नुहोस्।';

  @override
  String get free => 'निःशुल्क';

  @override
  String get buy => 'किन्नुहोस्';

  @override
  String get iPaid => 'मैले तिरें';

  @override
  String get getIt => 'लिनुहोस्';

  @override
  String get gettingIt => 'लिँदै…';

  @override
  String get notNow => 'अहिले होइन';

  @override
  String venuesFound(int count) {
    final intl.NumberFormat countNumberFormat =
        intl.NumberFormat.decimalPattern(localeName);
    final String countString = countNumberFormat.format(count);

    return '$countString भेन्यु';
  }

  @override
  String slotBookedOn(String time, String court) {
    return '$court मा $time बुक भइसकेको छ';
  }

  @override
  String weWillTellYou(String time) {
    return '$time खाली भयो भने हामी खबर गर्छौं।';
  }

  @override
  String freeSlots(int count) {
    final intl.NumberFormat countNumberFormat =
        intl.NumberFormat.decimalPattern(localeName);
    final String countString = countNumberFormat.format(count);

    return '$countString खाली';
  }

  @override
  String slotWaiting(String time) {
    return '$time, बुक भइसकेको। तपाईं पर्खँदै हुनुहुन्छ';
  }

  @override
  String slotWaitQuestion(String time) {
    return '$time बुक भइसकेको छ। पर्खने?';
  }

  @override
  String slotClosed(String time) {
    return '$time, बन्द';
  }

  @override
  String slotBook(String time, String price) {
    return '$time, $price। बुक गर्नुहोस्';
  }

  @override
  String playedOn(String date) {
    return '$date मा खेलेको';
  }

  @override
  String venueReplied(String venue) {
    return '$venue को जवाफ';
  }

  @override
  String membershipChecks(String venue) {
    return '$venue ले भुक्तानी जाँचेपछि तपाईंको सदस्यता सुरु हुन्छ।';
  }

  @override
  String get bookThisSlot => 'यो स्लट बुक गर्नुहोस्';

  @override
  String get slotUnavailable => 'यो स्लट बुक गर्न मिल्दैन';

  @override
  String get seeOtherSlots => 'अरू खाली स्लट हेर्नुहोस्';

  @override
  String get addDepositFirst => 'पहिले बुकिङ डिपोजिट थप्नुहोस्';

  @override
  String get thisVenue => 'यो भेन्यु';

  @override
  String get extras => 'थप सामान';

  @override
  String get extrasPaidAtVenue => 'बुकिङसँगै भेन्युमा तिर्ने।';

  @override
  String get useMembership => 'सदस्यता प्रयोग गर्नुहोस्';

  @override
  String get dontUseOne => 'प्रयोग नगर्ने';

  @override
  String get discountCode => 'छुट कोड';

  @override
  String get remove => 'हटाउनुहोस्';

  @override
  String get codeHint => 'जस्तै DASHAIN20';

  @override
  String get apply => 'लागू गर्नुहोस्';

  @override
  String get noteForVenue => 'भेन्युका लागि नोट';

  @override
  String get noteHint => 'जस्तै हामी १० जना छौं, बिब चाहिन्छ';

  @override
  String get workingOutPrice => 'मूल्य हिसाब हुँदै';

  @override
  String get court => 'कोर्ट';

  @override
  String get discount => 'छुट';

  @override
  String get total => 'जम्मा';

  @override
  String get payNowToConfirm => 'पक्का गर्न अहिले तिर्नुहोस्';

  @override
  String get atTheVenue => 'भेन्युमा';

  @override
  String get pay => 'भुक्तानी';

  @override
  String get strikeAdvanceNote =>
      'यो भेन्युमा हालै ढिलो रद्द गरेको वा नआएकोले यहाँ बढी अग्रिम तिर्नुपर्छ।';

  @override
  String get book => 'बुक गर्नुहोस्';

  @override
  String get booking_ => 'बुक हुँदै…';

  @override
  String depositExplain(String venue, String amount) {
    return '$venue ले खेलाडीलाई FutSlot मा $amount राख्न भन्छ। यो खेलमा खर्च हुँदैन: ढिलो रद्द गरे वा नआए भेन्युको शुल्क मात्र यसबाट तिरिन्छ, र कुनै खेल बुक नभएको बेला फिर्ता लिन सकिन्छ।';
  }

  @override
  String addAmount(String amount) {
    return '$amount थप्नुहोस्';
  }

  @override
  String gamesLeft(int count) {
    final intl.NumberFormat countNumberFormat =
        intl.NumberFormat.decimalPattern(localeName);
    final String countString = countNumberFormat.format(count);

    return '$countString बाँकी';
  }

  @override
  String bookAndPay(String amount) {
    return 'बुक गरेर $amount तिर्नुहोस्';
  }

  @override
  String holdWhilePay(int minutes) {
    final intl.NumberFormat minutesNumberFormat =
        intl.NumberFormat.decimalPattern(localeName);
    final String minutesString = minutesNumberFormat.format(minutes);

    return 'तपाईंले भेन्युलाई तिर्दासम्म स्लट $minutesString मिनेट होल्ड गरिन्छ।';
  }

  @override
  String priceEach(String price) {
    return 'प्रति वटा $price';
  }

  @override
  String oneLess(String item) {
    return '$item एउटा घटाउनुहोस्';
  }

  @override
  String oneMore(String item) {
    return '$item एउटा थप्नुहोस्';
  }

  @override
  String get yourBookings => 'तपाईंका बुकिङ';

  @override
  String get upcoming => 'आउँदै गरेका';

  @override
  String get past => 'पुराना';

  @override
  String get noGamesComingUp => 'आउँदै गरेको खेल छैन';

  @override
  String get nothingHereYet => 'अहिलेसम्म केही छैन';

  @override
  String get findCourtPitch => 'खाली कोर्ट खोजेर एक मिनेटमै बुक गर्नुहोस्।';

  @override
  String get pastGamesShowHere => 'खेलेका वा रद्द गरेका खेलहरू यहाँ देखिन्छन्।';

  @override
  String get showMore => 'थप देखाउनुहोस्';

  @override
  String get booking => 'बुकिङ';

  @override
  String get bookedSeeYou => 'बुक भयो। कोर्टमा भेटौंला!';

  @override
  String get bookedPayBelow => 'बुक भयो। पक्का गर्न तल अग्रिम तिर्नुहोस्।';

  @override
  String get messageTheVenue => 'भेन्युलाई सन्देश पठाउनुहोस्';

  @override
  String get splitTheCost => 'खर्च बाँड्नुहोस्';

  @override
  String get splitTheCostDetail => 'बराबर बाँड्नुहोस्, वा बाजी: हार्नेले तिर्छ';

  @override
  String get yourNote => 'तपाईंको नोट';

  @override
  String get cancelled => 'रद्द';

  @override
  String get cancelledRefund => 'तपाईंले तिरेको सबै फिर्ता आउँछ।';

  @override
  String get yourReview => 'तपाईंको समीक्षा';

  @override
  String get edit => 'सम्पादन';

  @override
  String get delete => 'मेटाउनुहोस्';

  @override
  String get editYourReview => 'समीक्षा सम्पादन गर्नुहोस्';

  @override
  String get reviewShownAs =>
      'अरू खेलाडीले यसलाई तपाईंको पहिलो नाम र थरको पहिलो अक्षरसहित देख्छन्।';

  @override
  String get cancel => 'रद्द गर्नुहोस्';

  @override
  String get paymentSent => 'भुक्तानी पठाइयो';

  @override
  String get payDirectly =>
      'भेन्युलाई सिधै तिर्नुहोस्, त्यसपछि ट्रान्ज्याक्सन आइडी दिनुहोस्। भेन्युले जाँचेर बुकिङ पक्का गर्छ।';

  @override
  String get timeToPayRanOut => 'तिर्ने समय सकियो';

  @override
  String get slotMayBeGone =>
      'स्लट अरू कसैले लिइसकेको हुन सक्छ। अझै खाली भए फेरि बुक गर्नुहोस्।';

  @override
  String get seeFreeSlots => 'खाली स्लट हेर्नुहोस्';

  @override
  String get payment => 'भुक्तानी';

  @override
  String get paid => 'तिरिएको';

  @override
  String get beingChecked => ' · जाँच हुँदै';

  @override
  String get started => ' · सुरु भयो';

  @override
  String get notReceived => ' · प्राप्त भएन';

  @override
  String get paymentCancelled => ' · रद्द';

  @override
  String get refunded => ' · फिर्ता गरियो';

  @override
  String get partlyRefunded => ' · आंशिक फिर्ता';

  @override
  String get cancelBooking => 'बुकिङ रद्द गर्नुहोस्';

  @override
  String get cancelThisBooking => 'यो बुकिङ रद्द गर्ने?';

  @override
  String get why => 'किन?';

  @override
  String get cancelling => 'रद्द हुँदै…';

  @override
  String get keepIt => 'राख्नुहोस्';

  @override
  String get freeToCancel => 'अहिले रद्द गर्न निःशुल्क छ।';

  @override
  String bookingRef(String reference) {
    return 'बुकिङ $reference';
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
    return '$day, $start देखि $end सम्म, $court';
  }

  @override
  String cancelledLate(String percent) {
    return 'ढिलो रद्द भएकाले भेन्युले $percent% शुल्क राख्यो।';
  }

  @override
  String cancelledLateDeposit(String percent) {
    return 'ढिलो रद्द भएकाले भेन्युले $percent% शुल्क राख्यो। अग्रिमले नपुगेको भाग तपाईंको डिपोजिटबाट कटियो।';
  }

  @override
  String howWasVenue(String venue) {
    return '$venue कस्तो लाग्यो?';
  }

  @override
  String venueChecking(String venue) {
    return '$venue ले जाँच गर्दैछ। त्यतिन्जेल स्लट होल्डमा रहन्छ, पक्का भएपछि हामी खबर गर्छौं।';
  }

  @override
  String payToConfirm(String amount) {
    return 'पक्का गर्न $amount तिर्नुहोस्';
  }

  @override
  String heldFor(int count, String time) {
    final intl.NumberFormat countNumberFormat =
        intl.NumberFormat.decimalPattern(localeName);
    final String countString = countNumberFormat.format(count);

    return '$time सम्म, अझै $countString मिनेट होल्डमा।';
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
    return '$method बाट फिर्ता · $state';
  }

  @override
  String get refundSent => 'पठाइयो';

  @override
  String get refundOnItsWay => 'आउँदैछ';

  @override
  String youGetBack(String amount) {
    return 'तपाईंलाई $amount फिर्ता आउँछ।';
  }

  @override
  String cancelCosts(String percent, String fee) {
    return 'अहिले रद्द गरे मूल्यको $percent% ($fee) लाग्छ।';
  }

  @override
  String venueKeeps(String amount) {
    return 'तपाईंले तिरेकोमध्ये भेन्युले $amount राख्छ।';
  }

  @override
  String venueKeepsRefund(String amount, String refund) {
    return 'तपाईंले तिरेकोमध्ये भेन्युले $amount राख्छ र तपाईंलाई $refund फिर्ता आउँछ।';
  }

  @override
  String fromDeposit(String amount) {
    return 'बढीमा $amount थप तपाईंको FutSlot डिपोजिटबाट कटिन्छ।';
  }

  @override
  String get bankAccount => 'बैंक खाता';

  @override
  String get enterSendTo => 'पठाउने नम्बर वा खाता लेख्नुहोस्।';

  @override
  String get toppedUp => 'रकम थपियो';

  @override
  String get takenBack => 'फिर्ता लिइयो';

  @override
  String get keptAfterAll => 'अन्ततः राखियो';

  @override
  String get bookingDeposit => 'बुकिङ डिपोजिट';

  @override
  String get inYourDeposit => 'तपाईंको डिपोजिटमा';

  @override
  String get deposit => 'डिपोजिट';

  @override
  String get depositFull => 'पूरा भयो: जुनसुकै भेन्यु बुक गर्न सक्नुहुन्छ।';

  @override
  String get backToBooking => 'बुकिङमा फर्कनुहोस्';

  @override
  String get keepDeposit => 'डिपोजिट राख्छु';

  @override
  String get topUpSent => 'रकम पठाइयो';

  @override
  String get topUpChecking =>
      'FutSlot ले जाँच गर्दैछ, प्रायः केही घण्टाभित्र। रकम आएपछि बुक गर्न सक्नुहुन्छ।';

  @override
  String get payFutSlotDirectly =>
      'FutSlot लाई सिधै तिर्नुहोस्, त्यसपछि ट्रान्ज्याक्सन आइडी दिनुहोस्।';

  @override
  String get history => 'इतिहास';

  @override
  String get takeDepositBack => 'डिपोजिट फिर्ता लिनुहोस्';

  @override
  String get takeItBack => 'फिर्ता लिनुहोस्';

  @override
  String get sendItToMy => 'पठाउने ठाउँ';

  @override
  String get bankNameAccount => 'बैंकको नाम र खाता नम्बर';

  @override
  String get number => 'नम्बर';

  @override
  String get asking => 'अनुरोध हुँदै…';

  @override
  String get askForItBack => 'फिर्ता माग्नुहोस्';

  @override
  String get back => 'पछाडि';

  @override
  String get membershipsIntro =>
      'भेन्युहरूमा किनेका खेल प्याक र पास। त्यहाँ बुक गर्दा प्रयोग हुन्छन्।';

  @override
  String get noMembershipsYet => 'अहिलेसम्म सदस्यता छैन';

  @override
  String get noMembershipsBody =>
      'खेल प्याक वा पास बेच्ने भेन्युले आफ्नो पेजमा राख्छन्।';

  @override
  String get active => 'सक्रिय';

  @override
  String get expired => 'म्याद सकियो';

  @override
  String get savedVenues => 'सेभ गरिएका भेन्यु';

  @override
  String get noSavedVenues => 'सेभ गरिएको भेन्यु छैन';

  @override
  String get noSavedVenuesBody =>
      'मन परेको भेन्युमा मुटुमा थिच्नुहोस्, यहाँ भेटिन्छ।';

  @override
  String get howWasYourGame => 'तपाईंको खेल कस्तो रह्यो?';

  @override
  String get reviewWindow => 'खेलेको ३० दिनसम्म समीक्षा लेख्न सकिन्छ।';

  @override
  String get review => 'समीक्षा';

  @override
  String get yourReviews => 'तपाईंका समीक्षा';

  @override
  String get noReviewsWritten =>
      'तपाईंले अहिलेसम्म कुनै भेन्युको समीक्षा लेख्नुभएको छैन।';

  @override
  String get reviewHidden => 'जाँच हुँदासम्म FutSlot ले लुकाएको';

  @override
  String get reviewRemoved => 'FutSlot ले हटाएको';

  @override
  String get waitlist => 'पर्खाइ सूची';

  @override
  String get waitlistIntro =>
      'अरूले बुक गरेका तर तपाईंलाई चाहिएका स्लट। खाली भयो भने हामी खबर गर्छौं, र जसले पहिले बुक गर्छ उसैले पाउँछ।';

  @override
  String get notWaiting => 'तपाईं कुनै स्लट पर्खिरहनुभएको छैन';

  @override
  String get notWaitingBody =>
      'भेन्युको पेजमा बुक भइसकेको स्लटमा थिचेर पर्खन सकिन्छ।';

  @override
  String get itFreedUp => 'खाली भयो। अरूले लिनुअघि बुक गर्नुहोस्।';

  @override
  String get stopWaiting => 'पर्खन छोड्नुहोस्';

  @override
  String get markAllRead => 'सबै पढिएको बनाउनुहोस्';

  @override
  String get nothingYet => 'अहिलेसम्म केही छैन';

  @override
  String get notificationsEmpty =>
      'बुकिङ पक्का, सम्झना र भेन्युका जवाफ यहाँ देखिन्छन्।';

  @override
  String get showOlder => 'पुराना देखाउनुहोस्';

  @override
  String get unread => 'नपढिएको';

  @override
  String feeToVenue(String venue, String reference) {
    return '$venue लाई शुल्क · $reference';
  }

  @override
  String depositIntro(String amount) {
    return 'केही भेन्युले FutSlot मा $amount राख्ने खेलाडीको मात्र बुकिङ लिन्छन्। यो खेलमा खर्च हुँदैन: ढिलो रद्द गरे वा नआए भेन्युको शुल्क मात्र यसबाट तिरिन्छ। त्यहाँ कुनै खेल बुक नभएको बेला जहिले पनि फिर्ता लिन सकिन्छ।';
  }

  @override
  String ofAmount(String amount) {
    return ' / $amount';
  }

  @override
  String comingBack(String amount) {
    return '$amount तपाईंलाई फिर्ता आउँदैछ';
  }

  @override
  String withdrawalWhen(String method, String sendTo, String date) {
    return 'FutSlot ले $date मा तपाईंको $method $sendTo मा पठाउँछ। त्यतिन्जेल डिपोजिट चाहिने भेन्यु बुक गर्न मिल्दैन।';
  }

  @override
  String get depositInUse => 'तपाईंको डिपोजिटले एउटा खेल ढाकेको छ';

  @override
  String get depositInUseWhen =>
      'डिपोजिट चाहिने भेन्युमा बुक गरेका खेल खेलिएपछि वा रद्द भएपछि फिर्ता लिन सकिन्छ।';

  @override
  String withdrawExplain(String amount) {
    return 'माग्नुभएको ३ दिनपछि FutSlot ले पूरै $amount पठाउँछ।';
  }

  @override
  String until(String date) {
    return '$date सम्म';
  }

  @override
  String ended(String date) {
    return '$date मा सकियो';
  }

  @override
  String get messages => 'सन्देशहरू';

  @override
  String get findAVenue => 'भेन्यु खोज्नुहोस्';

  @override
  String get noMessagesYet => 'अहिलेसम्म सन्देश छैन';

  @override
  String get noMessagesBody =>
      'कोर्ट, बुकिङ वा अरू जे पनि भेन्युलाई सोध्नुहोस्: भेन्युको पेज वा बुकिङमा \"सन्देश\" थिच्नुहोस्।';

  @override
  String get typeMessageFirst => 'पहिले सन्देश लेख्नुहोस्।';

  @override
  String get venue => 'भेन्यु';

  @override
  String get showEarlier => 'अघिल्ला सन्देश देखाउनुहोस्';

  @override
  String get send => 'पठाउनुहोस्';

  @override
  String get seen => ' · हेरियो';

  @override
  String get yesterday => 'हिजो';

  @override
  String nobodyMatches(String text) {
    return '\"$text\" सँग मिल्ने कोही छैन।';
  }

  @override
  String unreadCount(int count) {
    final intl.NumberFormat countNumberFormat =
        intl.NumberFormat.decimalPattern(localeName);
    final String countString = countNumberFormat.format(count);

    return '$countString नपढिएको';
  }

  @override
  String askVenueBooking(String venue) {
    return '$venue लाई कोर्ट, मूल्य वा यो बुकिङबारे सोध्नुहोस्। जवाफ यहीँ आउँछ, र हामी खबर गर्छौं।';
  }

  @override
  String askVenue(String venue) {
    return '$venue लाई कोर्ट, मूल्य वा बुकिङबारे सोध्नुहोस्। जवाफ यहीँ आउँछ, र हामी खबर गर्छौं।';
  }

  @override
  String messageTo(String venue) {
    return '$venue लाई सन्देश';
  }

  @override
  String get giveNameOrPhone =>
      'उहाँको नाम, वा FutSlot मा प्रयोग गर्ने फोन नम्बर दिनुहोस्।';

  @override
  String get changeSplit => 'खर्च कसरी बाँड्ने बदल्नुहोस्।';

  @override
  String get startSplitIntro =>
      'यो खेलको खर्च खेलाडीसँग बाँड्नुहोस्: बराबर, वा बाजी लगाएर।';

  @override
  String get whoPays => 'कसले तिर्ने';

  @override
  String get losingSidePays => 'हार्ने टिमले तिर्ने';

  @override
  String get percentOfEverything => '% सबैको';

  @override
  String get oneSide => 'एउटा टिम';

  @override
  String get otherSide => 'अर्को टिम';

  @override
  String get teamA => 'टिम A';

  @override
  String get teamB => 'टिम B';

  @override
  String get youPlayFor => 'तपाईं खेल्ने टिम';

  @override
  String get extrasRs => 'थप सामान (रु)';

  @override
  String get forWhat => 'केका लागि';

  @override
  String get extrasHint => 'पानी, पेय…';

  @override
  String get startSplitting => 'बाँड्न सुरु गर्नुहोस्';

  @override
  String get played => 'खेलियो';

  @override
  String get undo => 'फिर्ता लिनुहोस्';

  @override
  String get playedTheGame => 'खेल सकियो?';

  @override
  String get whoWon => 'कसले जित्यो?';

  @override
  String get worksOutShares =>
      'यसले सबैको भाग हिसाब गर्छ र एप भएका खेलाडीलाई खबर गर्छ।';

  @override
  String get workOutShares => 'भाग हिसाब गर्नुहोस्';

  @override
  String get addAPlayer => 'खेलाडी थप्नुहोस्';

  @override
  String get addPlayerHelp =>
      'FutSlot को फोन नम्बरले थपे उहाँले आफ्नो भाग देख्नुहुन्छ, नत्र नाम मात्र।';

  @override
  String get phone => 'फोन';

  @override
  String get add => 'थप्नुहोस्';

  @override
  String get eachPaysRoughly => 'हरेक खेलाडीले लगभग तिर्ने';

  @override
  String get ifResult => 'यदि';

  @override
  String get you => ' (तपाईं)';

  @override
  String get bookedBy => ' (बुक गर्ने)';

  @override
  String get whatYouOwe => 'तपाईंले तिर्नुपर्ने';

  @override
  String get duesIntro =>
      'अरूले बुक गरेका खेलमा तपाईंको भाग। तिरेपछि उहाँहरूले चिनो लगाउनुहुन्छ।';

  @override
  String get allSquare => 'तपाईंको कुनै बाँकी छैन';

  @override
  String get allSquareBody =>
      'कसैले आफूले बुक गरेको खेलमा तपाईंलाई थपेर खर्च बाँडेमा तपाईंको भाग यहाँ देखिन्छ।';

  @override
  String get inAll => ' जम्मा';

  @override
  String extrasFor(String note) {
    return 'थप सामान · $note';
  }

  @override
  String collectedSoFar(String collected, String outstanding) {
    return '$collected उठ्यो · $outstanding आउन बाँकी';
  }

  @override
  String shareOnceResult(String name) {
    return '$name ले नतिजा राखेपछि तपाईंको भाग हिसाब हुन्छ।';
  }

  @override
  String sharePaid(String name, String amount) {
    return 'तिरियो। तपाईंको $amount $name सँग छ।';
  }

  @override
  String youOwe(String name, String amount) {
    return 'तपाईंले $name लाई $amount तिर्नुपर्छ';
  }

  @override
  String payThemOn(String phone) {
    return 'eSewa वा Khalti मा तिर्नुहोस्: $phone। पैसा आएपछि उहाँले चिनो लगाउनुहुन्छ।';
  }

  @override
  String paidName(String name) {
    return 'तिरेको: $name';
  }

  @override
  String markPaidName(String name) {
    return 'तिरेको भनी चिनो: $name';
  }

  @override
  String moveToOtherSide(String name) {
    return '$name लाई अर्को टिममा सार्नुहोस्';
  }

  @override
  String removeName(String name) {
    return '$name लाई हटाउनुहोस्';
  }

  @override
  String payTo(String phone) {
    return '$phone मा तिर्नुहोस्';
  }

  @override
  String get full => 'भरियो';

  @override
  String get postOpen => 'खुला';

  @override
  String get postCalledOff => 'रद्द गरियो';

  @override
  String get postOver => 'सकियो';

  @override
  String get playersWanted => 'खेलाडी चाहियो';

  @override
  String get opponentWanted => 'प्रतिद्वन्द्वी चाहियो';

  @override
  String get placeToBeAgreed => 'ठाउँ पछि तय हुने';

  @override
  String get games_ => 'खेलहरू';

  @override
  String get gamesIntro =>
      'खेलाडी पुगेन, वा खेल्ने टिम खोज्दै? पोस्ट गर्नुहोस्, वा कुनैमा जोडिनुहोस्।';

  @override
  String get postAGame => 'खेल पोस्ट गर्नुहोस्';

  @override
  String get openGames => 'खुला खेलहरू';

  @override
  String get yours => 'तपाईंका';

  @override
  String get all => 'सबै';

  @override
  String get anyDay => 'जुनसुकै दिन';

  @override
  String get noPostedGames =>
      'तपाईंले कुनै खेल पोस्ट गर्नुभएको वा जोडिनुभएको छैन';

  @override
  String get noOpenGames => 'खुला खेल छैनन्';

  @override
  String get noGamesBody =>
      'पोस्ट गर्नुहोस्, नजिकका खेलाडीले जोडिन अनुरोध गर्न सक्छन्।';

  @override
  String get yourPost => 'तपाईंको पोस्ट';

  @override
  String get youAskedToJoin => 'तपाईंले जोडिन अनुरोध गर्नुभयो';

  @override
  String get youreIn => 'तपाईं समावेश हुनुभयो';

  @override
  String get notThisTime => 'यसपटक भएन';

  @override
  String get youWithdrew => 'तपाईंले फिर्ता लिनुभयो';

  @override
  String get game => 'खेल';

  @override
  String get noLongerTakingPlayers => 'यो खेलमा अब खेलाडी लिइँदैन।';

  @override
  String get askingToJoin => 'जोडिन अनुरोध';

  @override
  String get noOneYet => 'अहिलेसम्म कोही छैन। कसैले अनुरोध गरेमा खबर आउँछ।';

  @override
  String get forTeamBefore => '';

  @override
  String get forTeamAfter => ' का लागि';

  @override
  String get accept => 'स्वीकार गर्नुहोस्';

  @override
  String get decline => 'अस्वीकार गर्नुहोस्';

  @override
  String get accepted => 'स्वीकार भयो';

  @override
  String get declined => 'अस्वीकार भयो';

  @override
  String get withdrew => 'फिर्ता लियो';

  @override
  String get callGameOff => 'खेल रद्द गर्नुहोस्';

  @override
  String get youreInExclaim => 'तपाईं समावेश हुनुभयो!';

  @override
  String get withdraw => 'फिर्ता लिनुहोस्';

  @override
  String get playThemWithTeam => 'आफ्नो टिमसँग खेल्नुहोस्';

  @override
  String get askToJoin => 'जोडिन अनुरोध गर्नुहोस्';

  @override
  String get needCaptainToTakeOn =>
      'यो खेल्न तपाईं कुनै टिमको कप्तान हुनुपर्छ।';

  @override
  String get yourTeam => 'तपाईंको टिम';

  @override
  String get joinMessageHint => 'जस्तै गोलमा खेल्न सक्छु';

  @override
  String get askingEllipsis => 'अनुरोध हुँदै…';

  @override
  String get theySeeProfile => 'उहाँहरूले तपाईंको खेलाडी प्रोफाइल देख्नुहुन्छ।';

  @override
  String get editIt => 'सम्पादन गर्नुहोस्';

  @override
  String get sayWhereWhen =>
      'कहाँ (भेन्यु वा शहर) र कहिले भन्नुहोस्, वा आफ्नो कुनै बुकिङ छान्नुहोस्।';

  @override
  String get whatDoYouNeed => 'के चाहियो?';

  @override
  String get needPlayersOption => 'खेलाडी';

  @override
  String get needPlayersDetail => 'खेलका लागि खेलाडी पुगेनन्';

  @override
  String get needOpponentOption => 'प्रतिद्वन्द्वी';

  @override
  String get needOpponentDetail => 'हाम्रो टिमलाई खेल चाहियो';

  @override
  String get needCaptainForOpponent =>
      'प्रतिद्वन्द्वी खोज्न तपाईं कुनै टिमको कप्तान हुनुपर्छ।';

  @override
  String get whereAndWhen => 'कहाँ र कहिले';

  @override
  String get notBookedYet => 'अझै बुक गरिएको छैन';

  @override
  String get notDecidedCity => 'तय भएको छैन: शहर मात्र';

  @override
  String get city => 'शहर';

  @override
  String get cityHint => 'काठमाडौं';

  @override
  String get when => 'कहिले';

  @override
  String get pickTime => 'समय छान्नुहोस्';

  @override
  String get playersNeeded => 'चाहिने खेलाडी';

  @override
  String get eachPays => 'प्रत्येकले तिर्ने';

  @override
  String get level => 'स्तर';

  @override
  String get note => 'नोट';

  @override
  String get postNoteHint => 'जस्तै मैत्रीपूर्ण खेल, गोलकिपर चाहियो';

  @override
  String get postIt => 'पोस्ट गर्नुहोस्';

  @override
  String get posting => 'पोस्ट हुँदै…';

  @override
  String teamWantsGame(String team) {
    return '$team लाई खेल चाहियो';
  }

  @override
  String playersNeededCount(int count) {
    final intl.NumberFormat countNumberFormat =
        intl.NumberFormat.decimalPattern(localeName);
    final String countString = countNumberFormat.format(count);

    return '$countString खेलाडी चाहियो';
  }

  @override
  String yourPostAsking(int count) {
    final intl.NumberFormat countNumberFormat =
        intl.NumberFormat.decimalPattern(localeName);
    final String countString = countNumberFormat.format(count);

    return 'तपाईंको पोस्ट · $countString जनाले जोडिन अनुरोध गरे';
  }

  @override
  String seeVenue(String venue) {
    return '$venue हेर्नुहोस्';
  }

  @override
  String callOrMessage(String name, String phone) {
    return '$name लाई $phone मा फोन वा सन्देश गर्नुहोस्।';
  }

  @override
  String pickedSomeoneElse(String name) {
    return 'यसपटक $name ले अरू कसैलाई छान्नुभयो।';
  }

  @override
  String askedToJoinDecides(String name) {
    return 'तपाईंले जोडिन अनुरोध गर्नुभयो। $name ले निर्णय गर्नुहुन्छ, र तपाईंलाई खबर आउँछ।';
  }

  @override
  String get giveTeamName => 'टिमको नाम दिनुहोस्।';

  @override
  String get teams => 'टिमहरू';

  @override
  String get invitations => 'निमन्त्रणाहरू';

  @override
  String get join => 'जोडिनुहोस्';

  @override
  String get yourTeams => 'तपाईंका टिम';

  @override
  String get newTeam => 'नयाँ टिम';

  @override
  String get createTeam => 'टिम बनाउनुहोस्';

  @override
  String get creating => 'बन्दैछ…';

  @override
  String get youllBeCaptain =>
      'तपाईं कप्तान हुनुहुन्छ, र फोन नम्बरबाट खेलाडीलाई बोलाउन सक्नुहुन्छ।';

  @override
  String get notInTeam =>
      'तपाईं अहिलेसम्म कुनै टिममा हुनुहुन्न। टिम बनाउनुहोस्, वा कप्तानलाई बोलाउन भन्नुहोस्।';

  @override
  String get findATeam => 'टिम खोज्नुहोस्';

  @override
  String get teamName => 'टिमको नाम';

  @override
  String get search => 'खोज्नुहोस्';

  @override
  String get noPublicTeams => 'कुनै सार्वजनिक टिम भेटिएन।';

  @override
  String get enterTheirMobile => 'उहाँको १० अङ्कको मोबाइल नम्बर लेख्नुहोस्।';

  @override
  String get team => 'टिम';

  @override
  String get squad => 'खेलाडीहरू';

  @override
  String get invited => 'निमन्त्रणा गरिएको';

  @override
  String get inviteAPlayer => 'खेलाडी बोलाउनुहोस्';

  @override
  String get inviteHelp =>
      'उहाँले FutSlot मा प्रयोग गर्ने फोन नम्बरबाट। उहाँलाई खबर जान्छ र जोडिन सक्नुहुन्छ।';

  @override
  String get invite => 'बोलाउनुहोस्';

  @override
  String get leaveNeedsNewCaptain =>
      'छोड्न, पहिले अरू कसैलाई कप्तान बनाउनुहोस्।';

  @override
  String get cancelInvite => 'निमन्त्रणा रद्द गर्नुहोस्';

  @override
  String get profileIntro =>
      'खेल वा टिममा जोडिन अनुरोध गर्दा कप्तान र खेलाडीले यो देख्छन्।';

  @override
  String get position => 'पोजिसन';

  @override
  String get strongerFoot => 'बलियो खुट्टा';

  @override
  String get aboutYou => 'तपाईंको बारेमा';

  @override
  String get aboutYouHint => 'जस्तै विङमा छिटो, प्रायः शुक्रबार बेलुका खेल्छु';

  @override
  String get profilePublic => 'अरू खेलाडीलाई मेरो प्रोफाइल देखाउनुहोस्';

  @override
  String invitedToJoin(String team) {
    return 'तपाईंलाई $team मा निमन्त्रणा छ।';
  }

  @override
  String leaveTeam(String team) {
    return '$team छोड्नुहोस्';
  }

  @override
  String changeMember(String name) {
    return '$name लाई बदल्नुहोस्';
  }

  @override
  String makeRole(String role) {
    return '$role बनाउनुहोस्';
  }

  @override
  String gamesPlayedOnFutSlot(String games) {
    return 'FutSlot मा $games खेलेको';
  }

  @override
  String get tournaments => 'प्रतियोगिताहरू';

  @override
  String get comingUp => 'आउँदै';

  @override
  String get finished => 'सकिएका';

  @override
  String get yourTeamsTab => 'तपाईंका टिम';

  @override
  String get noTournamentsComingUp => 'आउँदै गरेको प्रतियोगिता छैन';

  @override
  String get noFinishedTournaments => 'अहिलेसम्म सकिएको प्रतियोगिता छैन';

  @override
  String get checkBackSoon => 'केही समयपछि फेरि हेर्नुहोस्।';

  @override
  String get finishedTournamentsShow =>
      'सकिएका प्रतियोगिता र विजेता यहाँ देखिन्छन्।';

  @override
  String get teamsNotEntered =>
      'तपाईंका टिमले अहिलेसम्म कुनै प्रतियोगितामा भाग लिएका छैनन्';

  @override
  String get teamsNotEnteredBody =>
      'टिमको कप्तान वा उपकप्तानले प्रतियोगिताको पेजबाट दर्ता गर्नुहुन्छ।';

  @override
  String get freeToEnter => 'निःशुल्क प्रवेश';

  @override
  String get vs => 'बनाम';

  @override
  String get venueCouldntFindPayment => 'भेन्युले यो भुक्तानी भेट्टाएन।';

  @override
  String get tournament => 'प्रतियोगिता';

  @override
  String get format => 'ढाँचा';

  @override
  String get entry => 'प्रवेश';

  @override
  String get teamsFact => 'टिमहरू';

  @override
  String get squads => 'खेलाडी संख्या';

  @override
  String get prizes => 'पुरस्कार';

  @override
  String get rules => 'नियम';

  @override
  String get table => 'तालिका';

  @override
  String get fixtures => 'खेल तालिका';

  @override
  String get teamsIn => 'सहभागी टिम';

  @override
  String get venueChecksPlace => 'भेन्युले जाँचेर तपाईंको स्थान पक्का गर्छ।';

  @override
  String get enterATeam => 'टिम दर्ता गर्नुहोस्';

  @override
  String get enterYourTeam => 'आफ्नो टिम दर्ता गर्नुहोस्';

  @override
  String get onlyCaptainCanEnter =>
      'टिमको कप्तान वा उपकप्तानले मात्र दर्ता गर्न सक्नुहुन्छ।';

  @override
  String get onlyActiveMembers =>
      'टिमका सक्रिय सदस्य मात्र खेल्न पाउँछन्। टिमको पेजबाट थप बोलाउनुहोस्।';

  @override
  String get enter => 'दर्ता गर्नुहोस्';

  @override
  String get entering => 'दर्ता हुँदै…';

  @override
  String get shirtNo => 'नं.';

  @override
  String get colTeam => 'टिम';

  @override
  String get colPlayed => 'खे';

  @override
  String get colWon => 'जि';

  @override
  String get colDrawn => 'ब';

  @override
  String get colLost => 'हा';

  @override
  String get colGoalDiff => 'गो.अ.';

  @override
  String get colPoints => 'अंक';

  @override
  String get toBeDecided => 'तय हुन बाँकी';

  @override
  String get walkover => 'वाकओभर';

  @override
  String penalties(String home, String away) {
    return ' (पेनाल्टी $home–$away)';
  }

  @override
  String perTeam(String amount) {
    return 'प्रति टिम $amount';
  }

  @override
  String teamsOfMax(String count, String max) {
    return '$count/$max टिम';
  }

  @override
  String groupName(String group) {
    return 'समूह $group';
  }

  @override
  String championWon(String team) {
    return '$team विजेता';
  }

  @override
  String teamsOf(String count, String max) {
    return '$max मध्ये $count';
  }

  @override
  String squadSize(String min, String max) {
    return '$min–$max खेलाडी';
  }

  @override
  String venueCheckingPlace(String venue) {
    return '$venue ले जाँच गर्दैछ, स्थान पक्का भएपछि हामी खबर गर्छौं।';
  }

  @override
  String lastPaymentRejected(String reason) {
    return 'तपाईंको पछिल्लो भुक्तानी स्वीकार भएन: $reason';
  }

  @override
  String payAmountTo(String amount, String venue) {
    return '$venue लाई $amount तिर्नुहोस्';
  }

  @override
  String entriesClose(String date) {
    return 'दर्ता $date मा बन्द हुन्छ।';
  }

  @override
  String squadCount(String count, String min, String max) {
    return 'खेलाडी ($min–$max मध्ये $count)';
  }

  @override
  String enterThenPay(String amount) {
    return 'दर्ता गरेर $amount तिर्नुहोस्';
  }

  @override
  String shirtNumberFor(String name) {
    return '$name को जर्सी नम्बर';
  }

  @override
  String pickAtLeastPlayers(int count) {
    final intl.NumberFormat countNumberFormat =
        intl.NumberFormat.decimalPattern(localeName);
    final String countString = countNumberFormat.format(count);

    return 'कम्तीमा $countString खेलाडी छान्नुहोस्।';
  }

  @override
  String get amenityParking => 'पार्किङ';

  @override
  String get amenityChangingRoom => 'लुगा फेर्ने कोठा';

  @override
  String get amenityShower => 'नुहाउने ठाउँ';

  @override
  String get amenityDrinkingWater => 'खानेपानी';

  @override
  String get amenityCafe => 'क्याफे / क्यान्टिन';

  @override
  String get amenityFloodlights => 'फ्लडलाइट';

  @override
  String get amenityWifi => 'वाइफाइ';

  @override
  String get amenityFirstAid => 'प्राथमिक उपचार';

  @override
  String get amenityEquipmentRental => 'सामान भाडामा';

  @override
  String get amenitySpectatorSeating => 'दर्शक बस्ने ठाउँ';
}
